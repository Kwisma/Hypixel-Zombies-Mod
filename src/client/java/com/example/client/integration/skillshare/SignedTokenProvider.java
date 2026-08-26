package com.example.client.integration.skillshare;

import com.example.ZombiesMod;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

/** Fetches and verifies the signed short-lived token used by SkillShare v1. */
final class SignedTokenProvider {

    private static final String PUBLIC_KEY_BASE64 =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1Mq147intdgg6rL2x4P/" +
            "pJxmkWHl1x8GUME7khtrA+/dLp+N0FeXnSfyg06JWvRgX3uW7t9A/GU481YKph8V" +
            "yviHmRJtgbYkT9LnXazlKR7uEnvkH5J8lVrYfvqzaMneb+bWndqPuGzR8c5563em" +
            "XnVBZgI2YjLtoabrlZi01z+C2HsrngP8yxH8xTIdOswajpFMU2HbVPTvMO3QOHE5" +
            "dFVOevnbH/q3QdDujmD0qkgJtflbthJoKTRe2FD0I9do600uoxUXELaSdd9v9JNP" +
            "d8xddF9Mv90fSIM+D58Zl5PEW7Uz4XeYYcsAl1eTweKONm3DIo2A3ZGwc4+wts0S" +
            "BwIDAQAB";

    private static final long REFRESH_MARGIN_MS = Duration.ofMinutes(5).toMillis();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);

    private final Gson gson = new Gson();
    private final HttpClient httpClient;
    private final URI tokenUri;
    private final AtomicBoolean refreshing = new AtomicBoolean();

    private volatile String cachedJson;
    private volatile long expiresAtMs;

    SignedTokenProvider(HttpClient httpClient, URI tokenUri) {
        this.httpClient = httpClient;
        this.tokenUri = tokenUri;
    }

    JsonObject getNonBlocking() {
        long now = System.currentTimeMillis();
        String json = cachedJson;
        if (json == null || expiresAtMs - now <= REFRESH_MARGIN_MS) {
            refreshAsync();
        }
        return json != null && expiresAtMs > now
                ? gson.fromJson(json, JsonObject.class)
                : null;
    }

    void refreshAsync() {
        if (!refreshing.compareAndSet(false, true)) {
            return;
        }

        HttpRequest request = HttpRequest.newBuilder(tokenUri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", "ZombiesMod-SkillShare/1.0")
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenAccept(this::acceptResponse)
                .exceptionally(error -> {
                    ZombiesMod.LOGGER.warn("SkillShare token request failed", error);
                    return null;
                })
                .whenComplete((unused, error) -> refreshing.set(false));
    }

    private void acceptResponse(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            ZombiesMod.LOGGER.warn("SkillShare token request returned HTTP {}", response.statusCode());
            return;
        }

        try {
            JsonObject token = gson.fromJson(response.body(), JsonObject.class);
            if (!verify(token)) {
                ZombiesMod.LOGGER.warn("SkillShare rejected a token with an invalid signature");
                return;
            }

            long expMs = Math.multiplyExact(token.get("exp").getAsLong(), 1000L);
            if (expMs <= System.currentTimeMillis()) {
                ZombiesMod.LOGGER.warn("SkillShare received an already-expired token");
                return;
            }

            cachedJson = response.body();
            expiresAtMs = expMs;
        } catch (Exception error) {
            ZombiesMod.LOGGER.warn("SkillShare failed to parse its signed token", error);
        }
    }

    private static boolean verify(JsonObject token) {
        try {
            if (token == null || !token.has("token") || !token.has("exp") || !token.has("sig")) {
                return false;
            }

            String message = token.get("token").getAsString() + ":" + token.get("exp").getAsLong();
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(loadPublicKey());
            verifier.update(message.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(token.get("sig").getAsString()));
        } catch (Exception ignored) {
            return false;
        }
    }

    private static PublicKey loadPublicKey() throws Exception {
        byte[] encoded = Base64.getDecoder().decode(PUBLIC_KEY_BASE64);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encoded));
    }
}
