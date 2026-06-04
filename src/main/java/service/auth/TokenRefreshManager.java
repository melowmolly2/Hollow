package service.auth;

import dto.auth.AuthResponse;
import dto.auth.RefreshTokenRequest;
import model.TokenStorage;
import network.ApiClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TokenRefreshManager {
    private static final long REFRESH_SKEW_MILLIS = 60_000L;
    private static final long RETRY_DELAY_MILLIS = 10_000L;
    private static final long MIN_DELAY_MILLIS = 1_000L;
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "token-refresh-scheduler");
        thread.setDaemon(true);
        return thread;
    });

    private static final Object LOCK = new Object();
    private static ScheduledFuture<?> scheduledRefresh;
    private static boolean refreshInFlight;

    private TokenRefreshManager() {
    }

    public static void start() {
        scheduleNextRefresh();
    }

    public static void stop() {
        synchronized (LOCK) {
            cancelScheduledRefresh();
            refreshInFlight = false;
        }
    }

    public static void scheduleNextRefresh() {
        synchronized (LOCK) {
            cancelScheduledRefresh();

            if (!TokenStorage.hasRefreshToken() || !TokenStorage.hasAccessToken()) {
                return;
            }

            long delayMillis = Math.max(MIN_DELAY_MILLIS,
                    TokenStorage.accessTokenExpiresAtMillis() - System.currentTimeMillis() - REFRESH_SKEW_MILLIS);
            scheduledRefresh = SCHEDULER.schedule(TokenRefreshManager::refreshNow, delayMillis, TimeUnit.MILLISECONDS);
        }
    }

    private static void refreshNow() {
        String refreshToken;
        synchronized (LOCK) {
            if (refreshInFlight) {
                return;
            }

            refreshToken = TokenStorage.getRefreshToken();
            if (refreshToken == null || refreshToken.isBlank()) {
                return;
            }

            refreshInFlight = true;
        }

        ApiClient.publicApi.refresh(new RefreshTokenRequest(refreshToken)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                synchronized (LOCK) {
                    refreshInFlight = false;
                }

                if (response.isSuccessful() && response.body() != null) {
                    TokenStorage.setTokens(response.body());
                    scheduleNextRefresh();
                    return;
                }

                TokenStorage.clear();
                stop();
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable throwable) {
                synchronized (LOCK) {
                    refreshInFlight = false;
                    cancelScheduledRefresh();
                    if (TokenStorage.hasRefreshToken()) {
                        scheduledRefresh = SCHEDULER.schedule(TokenRefreshManager::refreshNow,
                                RETRY_DELAY_MILLIS, TimeUnit.MILLISECONDS);
                    }
                }
            }
        });
    }

    private static void cancelScheduledRefresh() {
        if (scheduledRefresh != null) {
            scheduledRefresh.cancel(false);
            scheduledRefresh = null;
        }
    }
}
