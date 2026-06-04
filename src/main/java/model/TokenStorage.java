package model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dto.auth.AuthResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TokenStorage {
    public static String accessToken;
    public static String refreshToken;
    public static String username;

    private static long accessTokenExpiresAtMillis;

    public static synchronized void setSession(String username, AuthResponse auth) {
        TokenStorage.username = username;
        setTokens(auth);
    }

    public static synchronized void setTokens(AuthResponse auth) {
        if (auth == null) {
            clear();
            return;
        }

        accessToken = auth.accessToken;
        refreshToken = auth.refreshToken;
        accessTokenExpiresAtMillis = extractExpiryMillis(auth.accessToken);
    }

    public static synchronized void clear() {
        accessToken = null;
        refreshToken = null;
        username = null;
        accessTokenExpiresAtMillis = 0L;
    }

    public static synchronized String getAccessToken() {
        return accessToken;
    }

    public static synchronized String getRefreshToken() {
        return refreshToken;
    }

    public static synchronized String authorizationHeader() {
        if (accessToken == null || accessToken.isBlank()) {
            return null;
        }

        return "Bearer " + accessToken;
    }

    public static synchronized boolean hasRefreshToken() {
        return refreshToken != null && !refreshToken.isBlank();
    }

    public static synchronized boolean shouldRefreshAccessToken(long refreshSkewMillis) {
        if (!hasRefreshToken()) {
            return false;
        }

        if (accessToken == null || accessToken.isBlank()) {
            return true;
        }

        if (accessTokenExpiresAtMillis <= 0L) {
            return true;
        }

        return System.currentTimeMillis() + refreshSkewMillis >= accessTokenExpiresAtMillis;
    }

    private static long extractExpiryMillis(String token) {
        if (token == null || token.isBlank()) {
            return 0L;
        }

        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return 0L;
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            String payload = new String(payloadBytes, StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
            if (!json.has("exp") || json.get("exp").isJsonNull()) {
                return 0L;
            }

            return json.get("exp").getAsLong() * 1000L;
        } catch (RuntimeException ignored) {
            return 0L;
        }
    }
}
