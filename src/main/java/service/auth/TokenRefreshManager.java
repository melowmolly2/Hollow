package service.auth;

import dto.auth.AuthResponse;
import dto.auth.RefreshTokenRequest;
import model.TokenStorage;
import network.ApiClient;
import retrofit2.Response;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class TokenRefreshManager {
    private static final long REFRESH_SKEW_MILLIS = 120_000L;
    private static final Object REFRESH_LOCK = new Object();

    private TokenRefreshManager() {
    }

    public static boolean refreshIfNeededBlocking() {
        if (!TokenStorage.shouldRefreshAccessToken(REFRESH_SKEW_MILLIS)) {
            return TokenStorage.getAccessToken() != null && !TokenStorage.getAccessToken().isBlank();
        }

        return refreshBlocking();
    }

    public static boolean refreshBlocking() {
        return refreshBlocking(false);
    }

    public static boolean forceRefreshBlocking() {
        return refreshBlocking(true);
    }

    private static boolean refreshBlocking(boolean force) {
        synchronized (REFRESH_LOCK) {
            if (!force && !TokenStorage.shouldRefreshAccessToken(REFRESH_SKEW_MILLIS)) {
                return TokenStorage.getAccessToken() != null && !TokenStorage.getAccessToken().isBlank();
            }

            String refreshToken = TokenStorage.getRefreshToken();
            if (refreshToken == null || refreshToken.isBlank()) {
                TokenStorage.clear();
                return false;
            }

            try {
                Response<AuthResponse> response = ApiClient.publicApi
                        .refresh(new RefreshTokenRequest(refreshToken))
                        .execute();

                if (response.isSuccessful() && response.body() != null) {
                    TokenStorage.setTokens(response.body());
                    return true;
                }

                TokenStorage.clear();
                return false;
            } catch (IOException e) {
                return false;
            }
        }
    }

    public static void refreshAsync(TokenRefreshCallback callback) {
        CompletableFuture
                .supplyAsync(TokenRefreshManager::forceRefreshBlocking)
                .thenAccept(success -> {
                    if (success) {
                        callback.onSuccess();
                    } else {
                        callback.onError("Session expired. Please login again.");
                    }
                });
    }
}
