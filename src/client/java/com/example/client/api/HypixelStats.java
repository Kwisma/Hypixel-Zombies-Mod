package com.example.client.api;

import com.example.client.config.ZombiesConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public final class HypixelStats {
    public record Result(String text, boolean loading, boolean error) {}

    private static final Map<UUID, Result> RESULTS = new ConcurrentHashMap<>();

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "ZombiesMod-Stats");
        t.setDaemon(true);
        return t;
    });

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private HypixelStats() {}


    public static Result get(UUID uuid) {
        return RESULTS.get(uuid);
    }

    public static void query(String name, UUID uuid) {
        String key = ZombiesConfig.apiKey;
        if (key == null || key.isEmpty()) {
            RESULTS.put(uuid, new Result("缺少 API Key", false, true));
            return;
        }
        RESULTS.put(uuid, new Result("查询中...", true, false));

        EXECUTOR.submit(() -> {
            Result r;
            try {
                r = fetch(uuid, key);
            } catch (Exception e) {
                r = new Result("失败: " + e.getMessage(), false, true);
            }
            RESULTS.put(uuid, r);
        });
    }

    private static Result fetch(UUID uuid, String apiKey) throws Exception {
        String dashless = uuid.toString().replace("-", "");

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.hypixel.net/v2/player?uuid=" + dashless))
                .header("API-Key", apiKey)
                .header("User-Agent", "ZombiesMod/1.0")
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        int code = resp.statusCode();
        if (code == 403) return new Result("API Key 无效/过期", false, true);
        if (code == 429) return new Result("请求过快(限流)", false, true);
        if (code != 200) return new Result("HTTP " + code, false, true);

        JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
        if (root.has("success") && !root.get("success").getAsBoolean()) {
            String cause = root.has("cause") ? root.get("cause").getAsString() : "unknown";
            return new Result("错误: " + cause, false, true);
        }
        if (!root.has("player") || root.get("player").isJsonNull()) {
            return new Result("无玩家数据(改名/Nick?)", false, true);
        }

        JsonObject player = root.getAsJsonObject("player");
        if (!player.has("stats") || player.get("stats").isJsonNull()
                || !player.getAsJsonObject("stats").has("Arcade")) {
            return new Result("没玩过 Zombies", false, false);
        }

        JsonObject arcade = player.getAsJsonObject("stats").getAsJsonObject("Arcade");

        int aaBest = getInt(arcade, "best_round_zombies_alienarcadium"); // AA 最佳回合
        int aaWins = getInt(arcade, "wins_zombies_alienarcadium");        // AA 胜利数
        int totalRounds = getInt(arcade, "total_rounds_survived_zombies"); // 总回合数
        long kills = getLong(arcade, "zombie_kills_zombies");              // 总击杀
        long deaths = getLong(arcade, "deaths_zombies");                   // 总死亡
        double kd = deaths == 0 ? kills : (double) kills / deaths;

        String text = "AA BR" + aaBest
                + " · AA胜 " + aaWins
                + " · 总R " + fmt(totalRounds)
                + " · Kills " + fmt(kills)
                + " · KD " + String.format("%.2f", kd);
        return new Result(text, false, false);
    }

    private static int getInt(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsInt() : 0;
    }

    private static long getLong(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsLong() : 0L;
    }

    private static String fmt(long n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000) return String.format("%.1fK", n / 1_000.0);
        return Long.toString(n);
    }
}
