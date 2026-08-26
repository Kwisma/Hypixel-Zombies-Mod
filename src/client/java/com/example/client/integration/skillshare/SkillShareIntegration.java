package com.example.client.integration.skillshare;

import com.example.ZombiesMod;
import com.example.client.tracker.ServerTracker;
import com.example.client.utils.ChatUtils;
import com.example.client.utils.IMinecraft;
import com.example.client.utils.PlayerUtils;
import com.example.client.utils.ScoreboardUtils;
import com.example.client.utils.ZombiesMap;
import com.example.client.utils.ZombiesUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Main-thread orchestration for the public SkillShare-MICx v1 protocol. */
public final class SkillShareIntegration implements IMinecraft {

    private static final URI SERVER_URI = URI.create("wss://zombie.nienie.fun/zombies/ws");
    private static final URI TOKEN_URI = URI.create("https://zombie.nienie.fun/zombies/token");

    private static final long STATE_INTERVAL_MS = 250L;
    private static final long HEARTBEAT_INTERVAL_MS = 5_000L;
    private static final long JOIN_RETRY_MS = 4_000L;
    private static final long REMOTE_STATE_TTL_MS = 10_000L;
    private static final int INBOUND_BUDGET_PER_TICK = 256;

    private static final Pattern LR_RELEASE =
            Pattern.compile("^You struck (.+) with your Lightning Rod Skill!$", Pattern.CASE_INSENSITIVE);
    private static final Pattern HIT_COUNT = Pattern.compile("^(\\d+)(?:\\s+enemies?)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEAL_SELF = Pattern.compile(
            "^You healed yourself(?: and (\\d+) teammate[s]?)? with your Heal Skill!$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern HEAL_ON_ME = Pattern.compile(
            "^([A-Za-z0-9_]{1,16}) healed you with their Heal Skill!$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAYER_NAME = Pattern.compile("^[A-Za-z0-9_]{1,16}$");

    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private final SignedTokenProvider tokenProvider = new SignedTokenProvider(httpClient, TOKEN_URI);
    private final SkillShareConnection connection = new SkillShareConnection(httpClient, SERVER_URI);
    private final LocalSkillTracker localSkill = new LocalSkillTracker();

    private final Set<String> roomMembers = new HashSet<>();
    private final Map<String, RemoteSkill> remoteSkills = new HashMap<>();
    private final Map<String, Long> lastStateSequence = new HashMap<>();

    private boolean running;
    private boolean eligible;
    private boolean joined;
    private String selfName;
    private Set<String> lastRoster = Set.of();
    private Object lastLevel;
    private long pendingJoinAt;
    private long lastStateSentAt;
    private long lastHeartbeatAt;
    private long nextConnectAt;
    private long reconnectDelayMs = 1_000L;
    private long stateSequence;

    private static final class RemoteSkill {
        String state = "UNKNOWN";
        String name;
        int remainingSeconds;
        long receivedAt;
        boolean fiveSecondNoticeSent;
    }

    public void start() {
        running = true;
        nextConnectAt = 0L;
    }

    public void stop() {
        running = false;
        leaveAndDisconnect();
    }

    public void onGameDisconnect() {
        leaveAndDisconnect();
        lastLevel = null;
        localSkill.reset();
    }

    public void tick() {
        if (!running) {
            return;
        }

        long now = System.currentTimeMillis();
        if (mc.player == null || mc.level == null) {
            leaveAndDisconnect();
            return;
        }

        if (lastLevel != mc.level) {
            lastLevel = mc.level;
            localSkill.reset();
            resetRoomState();
            connection.disconnect();
            nextConnectAt = 0L;
        }

        Set<String> roster = readRoster();
        boolean shouldBeEligible = PlayerUtils.isInHypZombies() && roster.size() > 1;
        if (!shouldBeEligible) {
            if (eligible || connection.isConnected()) {
                leaveAndDisconnect();
            }
            eligible = false;
            return;
        }
        eligible = true;

        observeLocalSkill(now);
        tokenProvider.getNonBlocking();
        maintainConnection(now);
        processInbound(now);
        expireRemoteStates(now);

        if (!connection.isConnected()) {
            return;
        }

        if (!roster.equals(lastRoster)) {
            lastRoster = Set.copyOf(roster);
            joined = false;
            pendingJoinAt = 0L;
            roomMembers.clear();
            remoteSkills.clear();
            lastStateSequence.clear();
        }

        if (now - lastHeartbeatAt >= HEARTBEAT_INTERVAL_MS) {
            lastHeartbeatAt = now;
            sendSimpleType("heartbeat");
        }

        if (!joined) {
            if (pendingJoinAt == 0L || now - pendingJoinAt >= JOIN_RETRY_MS) {
                sendJoin(roster, now);
            }
            return;
        }

        if (now - lastStateSentAt >= STATE_INTERVAL_MS) {
            lastStateSentAt = now;
            sendState(now);
        }
    }

    public void onChat(String rawMessage) {
        if (!running || !eligible || rawMessage == null || selfName == null) {
            return;
        }

        String message = rawMessage.replaceAll("§.", "").trim();
        long now = System.currentTimeMillis();

        Matcher lr = LR_RELEASE.matcher(message);
        if (lr.matches()) {
            String target = lr.group(1).trim();
            Matcher countMatcher = HIT_COUNT.matcher(target);
            int struckCount = countMatcher.matches() ? Integer.parseInt(countMatcher.group(1)) : -1;
            String struckName = struckCount >= 0 ? null : target;
            localSkill.markReleased("Lightning Rod", now);
            if (joined) {
                sendLrRelease(struckCount, struckName);
                lastStateSentAt = 0L;
            }
            return;
        }

        if (HEAL_SELF.matcher(message).matches()) {
            localSkill.markReleased("Heal", now);
            if (joined) {
                sendHealRelease(selfName, now);
                lastStateSentAt = 0L;
            }
            return;
        }

        Matcher healed = HEAL_ON_ME.matcher(message);
        if (healed.matches()) {
            String healer = healed.group(1);
            // A player without SkillShare cannot report their own Heal. The
            // protocol permits a healed client to proxy it; the server dedupes.
            if (joined && !roomMembers.contains(healer)) {
                sendHealRelease(healer, now);
            }
        }
    }

    private void observeLocalSkill(long now) {
        ItemStack stack = mc.player.getInventory().getItem(LocalSkillTracker.SLOT_INDEX);
        localSkill.observe(stack, now);
    }

    private Set<String> readRoster() {
        LinkedHashSet<String> roster = new LinkedHashSet<>();
        selfName = mc.player.getName().getString();
        if (isValidPlayerName(selfName)) {
            roster.add(selfName);
        }
        for (ScoreboardUtils.ScorePlayer player : ScoreboardUtils.getZombiesPlayers()) {
            if (isValidPlayerName(player.name())) {
                roster.add(player.name());
            }
        }
        return roster;
    }

    private void maintainConnection(long now) {
        if (connection.consumeConnectedSignal()) {
            reconnectDelayMs = 1_000L;
            joined = false;
            pendingJoinAt = 0L;
            roomMembers.clear();
        }

        if (connection.isConnected() || now < nextConnectAt) {
            return;
        }

        connection.connect();
        nextConnectAt = now + reconnectDelayMs;
        reconnectDelayMs = Math.min(reconnectDelayMs * 2L, 30_000L);
    }

    private void sendJoin(Set<String> roster, long now) {
        JsonObject token = tokenProvider.getNonBlocking();
        if (token == null) {
            return;
        }

        JsonObject message = new JsonObject();
        message.addProperty("type", "join");
        message.add("token", token);
        message.addProperty("self", selfName);
        JsonArray rosterJson = new JsonArray();
        roster.forEach(rosterJson::add);
        message.add("roster", rosterJson);
        message.addProperty("map", displayMap(ZombiesUtils.getMap()));
        message.addProperty("round", Math.max(0, ServerTracker.currentRound));
        if (send(message)) {
            pendingJoinAt = now;
        }
    }

    private void sendState(long now) {
        LocalSkillTracker.Snapshot skill = localSkill.snapshot(now);
        JsonObject message = new JsonObject();
        message.addProperty("type", "state");
        message.addProperty("name", selfName);
        message.addProperty("state_seq", ++stateSequence);

        JsonObject position = new JsonObject();
        position.addProperty("x", mc.player.getX());
        position.addProperty("y", mc.player.getY());
        position.addProperty("z", mc.player.getZ());
        message.add("pos", position);

        if (skill.known()) {
            JsonObject skillJson = new JsonObject();
            skillJson.addProperty("slot", LocalSkillTracker.SLOT_INDEX);
            skillJson.addProperty("state", skill.state().name());
            skillJson.addProperty("name", skill.name());
            skillJson.addProperty("remaining_s", skill.remainingSeconds());
            message.add("skill", skillJson);

            if ("Lightning Rod".equals(skill.name())) {
                JsonObject lightningRod = new JsonObject();
                lightningRod.addProperty("released", skill.releasedAtMs());
                lightningRod.addProperty("ready_at", skill.readyAtMs());
                message.add("lr", lightningRod);
            }
        }

        send(message);
    }

    private void sendLrRelease(int struckCount, String struckName) {
        JsonObject message = new JsonObject();
        message.addProperty("type", "lr_release");
        message.addProperty("name", selfName);
        if (struckCount >= 0) {
            message.addProperty("struck_count", struckCount);
        } else if (struckName != null && !struckName.isBlank()) {
            message.addProperty("struck_name", struckName);
        }
        send(message);
    }

    private void sendHealRelease(String healer, long releasedAt) {
        JsonObject message = new JsonObject();
        message.addProperty("type", "heal_release");
        message.addProperty("healer", healer);
        message.addProperty("released", releasedAt);
        send(message);
    }

    private void processInbound(long now) {
        int budget = INBOUND_BUDGET_PER_TICK;
        String raw;
        while (budget-- > 0 && (raw = connection.poll()) != null) {
            try {
                JsonObject message = gson.fromJson(raw, JsonObject.class);
                if (message == null || !message.has("type")) {
                    continue;
                }
                switch (message.get("type").getAsString()) {
                    case "room_info" -> handleRoomInfo(message);
                    case "state" -> handleState(message, now);
                    case "member_left" -> handleMemberLeft(message);
                    case "lr_release" -> handleLrRelease(message);
                    case "heal_release" -> handleHealRelease(message);
                    default -> {
                    }
                }
            } catch (Exception error) {
                ZombiesMod.LOGGER.warn("SkillShare ignored an invalid inbound message", error);
            }
        }
    }

    private void handleRoomInfo(JsonObject message) {
        boolean wasJoined = joined;
        roomMembers.clear();
        JsonArray members = message.has("members") && message.get("members").isJsonArray()
                ? message.getAsJsonArray("members") : null;
        if (members != null) {
            for (JsonElement member : members) {
                String name = member.getAsString();
                if (isValidPlayerName(name)) {
                    roomMembers.add(name);
                }
            }
        }
        joined = selfName != null && roomMembers.contains(selfName);
        if (joined) {
            pendingJoinAt = 0L;
        }
        remoteSkills.keySet().removeIf(name -> !roomMembers.contains(name));
        lastStateSequence.keySet().removeIf(name -> !roomMembers.contains(name));

        if (joined && !wasJoined) {
            List<String> sorted = new ArrayList<>(roomMembers);
            sorted.sort(String.CASE_INSENSITIVE_ORDER);
            ChatUtils.print(ChatFormatting.GOLD + "[SkillShare] " + ChatFormatting.YELLOW
                    + "Connected: " + ChatFormatting.WHITE + String.join(", ", sorted));
        }
    }

    private void handleState(JsonObject message, long now) {
        String name = stringOrNull(message, "name");
        if (!isValidPlayerName(name) || name.equals(selfName) || !roomMembers.contains(name)) {
            return;
        }

        long sequence = message.has("state_seq") ? message.get("state_seq").getAsLong() : -1L;
        Long previous = lastStateSequence.get(name);
        if (sequence >= 0L && previous != null && sequence <= previous) {
            return;
        }
        if (sequence >= 0L) {
            lastStateSequence.put(name, sequence);
        }

        if (!message.has("skill") || !message.get("skill").isJsonObject()) {
            return;
        }
        JsonObject skillJson = message.getAsJsonObject("skill");
        String newState = stringOrDefault(skillJson, "state", "UNKNOWN");
        String newName = stringOrNull(skillJson, "name");
        int remaining = skillJson.has("remaining_s") ? skillJson.get("remaining_s").getAsInt() : 0;

        RemoteSkill skill = remoteSkills.computeIfAbsent(name, ignored -> new RemoteSkill());
        if (!java.util.Objects.equals(skill.name, newName) || "READY".equals(newState)
                || "UNKNOWN".equals(newState)) {
            skill.fiveSecondNoticeSent = false;
        }
        skill.state = newState;
        skill.name = newName;
        skill.remainingSeconds = Math.max(0, remaining);
        skill.receivedAt = now;

        if ("COOLING".equals(skill.state) && skill.name != null
                && skill.remainingSeconds > 0 && skill.remainingSeconds <= 5
                && !skill.fiveSecondNoticeSent) {
            skill.fiveSecondNoticeSent = true;
            ChatUtils.print(ChatFormatting.GOLD + "[SkillShare] " + ChatFormatting.WHITE + name
                    + ChatFormatting.YELLOW + "'s " + ChatFormatting.AQUA + skill.name
                    + ChatFormatting.YELLOW + " cooldown: " + ChatFormatting.RED
                    + skill.remainingSeconds + "s");
        }
    }

    private void handleMemberLeft(JsonObject message) {
        String name = stringOrNull(message, "name");
        if (name == null) return;
        roomMembers.remove(name);
        remoteSkills.remove(name);
        lastStateSequence.remove(name);
        if (name.equals(selfName)) {
            joined = false;
            pendingJoinAt = 0L;
        }
    }

    private void handleLrRelease(JsonObject message) {
        String name = stringOrNull(message, "name");
        if (!isValidPlayerName(name) || name.equals(selfName) || !roomMembers.contains(name)) {
            return;
        }
        String target;
        if (message.has("struck_count")) {
            target = message.get("struck_count").getAsInt() + " enemies";
        } else {
            target = stringOrDefault(message, "struck_name", "an enemy");
        }
        ChatUtils.print(ChatFormatting.GOLD + "[SkillShare] " + ChatFormatting.WHITE + name
                + ChatFormatting.YELLOW + " used LR and struck " + ChatFormatting.LIGHT_PURPLE + target);
    }

    private void handleHealRelease(JsonObject message) {
        String healer = stringOrNull(message, "healer");
        if (!isValidPlayerName(healer) || healer.equals(selfName)) {
            return;
        }
        ChatUtils.print(ChatFormatting.GOLD + "[SkillShare] " + ChatFormatting.WHITE + healer
                + ChatFormatting.YELLOW + " used HEAL");
    }

    private void expireRemoteStates(long now) {
        Set<String> expired = new HashSet<>();
        remoteSkills.forEach((name, skill) -> {
            if (now - skill.receivedAt > REMOTE_STATE_TTL_MS) {
                expired.add(name);
            }
        });
        expired.forEach(remoteSkills::remove);
    }

    private void leaveAndDisconnect() {
        if (joined && connection.isConnected()) {
            sendSimpleType("leave");
        }
        connection.disconnect();
        resetRoomState();
        reconnectDelayMs = 1_000L;
        nextConnectAt = 0L;
        eligible = false;
    }

    private void resetRoomState() {
        joined = false;
        pendingJoinAt = 0L;
        lastStateSentAt = 0L;
        lastHeartbeatAt = 0L;
        lastRoster = Set.of();
        roomMembers.clear();
        remoteSkills.clear();
        lastStateSequence.clear();
        stateSequence = 0L;
    }

    private void sendSimpleType(String type) {
        JsonObject message = new JsonObject();
        message.addProperty("type", type);
        send(message);
    }

    private boolean send(JsonObject message) {
        return connection.send(gson.toJson(message));
    }

    private static boolean isValidPlayerName(String name) {
        return name != null && PLAYER_NAME.matcher(name).matches();
    }

    private static String stringOrNull(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : null;
    }

    private static String stringOrDefault(JsonObject object, String key, String fallback) {
        String value = stringOrNull(object, key);
        return value == null ? fallback : value;
    }

    private static String displayMap(ZombiesMap map) {
        return switch (map) {
            case DEAD_END -> "Dead End";
            case BAD_BLOOD -> "Bad Blood";
            case ALIEN_ARCADIUM -> "Alien Arcadium";
            case THE_LAB -> "The Lab";
            case PRISON -> "Prison";
            case NULL -> "Zombies";
        };
    }
}
