package service.auth;

import dto.auth.LoginRequest;
import dto.auth.RefreshTokenRequest;
import dto.auth.RegisterRequest;
import dto.auth.AuthResponse;
import dto.common.BaseResponse;
import network.ApiClient;
import model.TokenStorage;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthService {
    public void logout(LogoutCallback callback) {
        if (TokenStorage.accessToken == null || TokenStorage.accessToken.isBlank()) {
            clearTokens();
            callback.onSuccess("Logged out.");
            return;
        }

        String authorization = "Bearer " + TokenStorage.accessToken;
        ApiClient.api.logout(authorization).enqueue(new Callback<BaseResponse>() {
            @Override
            public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String message = response.body().message;
                    clearTokens();
                    callback.onSuccess(message == null || message.isBlank() ? "Logged out." : message);
                    return;
                }

                clearTokens();
                callback.onSuccess("Logged out.");
            }

            @Override
            public void onFailure(Call<BaseResponse> call, Throwable throwable) {
                clearTokens();
                callback.onSuccess("Logged out.");
            }
        });
    }

    public void refreshToken(TokenRefreshCallback callback) {
        if (TokenStorage.refreshToken == null || TokenStorage.refreshToken.isBlank()) {
            callback.onError("Session expired. Please login again.");
            return;
        }

        RefreshTokenRequest request = new RefreshTokenRequest(TokenStorage.refreshToken);
        ApiClient.api.refresh(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse auth = response.body();
                    TokenStorage.accessToken = auth.accessToken;
                    TokenStorage.refreshToken = auth.refreshToken;
                    callback.onSuccess();
                    return;
                }

                clearTokens();
                callback.onError("Session expired. Please login again.");
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable throwable) {
                callback.onError("Network error: " + throwable.getMessage());
            }
        });
    }

    public void login(String username, String password, LoginCallback callback) {
        if (username == null || username.isBlank()) {
            callback.onError("Username is empty");
            return;
        }

        if (password == null || password.isBlank()) {
            callback.onError("Password is empty");
            return;
        }

        LoginRequest request = new LoginRequest(username, password);
        ApiClient.api.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse auth = response.body();
                    TokenStorage.accessToken = auth.accessToken;
                    TokenStorage.refreshToken = auth.refreshToken;
                    TokenStorage.username = username;
                    callback.onSuccess(auth);
                    return;
                }

                callback.onError("Login failed. HTTP code: " + response.code());
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable throwable) {
                callback.onError("Network error: " + throwable.getMessage());
            }
        });
    }

    public void register(String username, String displayName, String password, RegisterCallBack callback) {
        if (username == null || username.isBlank()) {
            callback.onError("Username is empty");
            return;
        }

        if (displayName == null || displayName.isBlank()) {
            callback.onError("Display name is empty");
            return;
        }

        if (password == null || password.isBlank()) {
            callback.onError("Password is empty");
            return;
        }

        RegisterRequest request = new RegisterRequest(username, displayName, password);
        ApiClient.api.register(request).enqueue(new Callback<BaseResponse>() {
            @Override
            public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().message);
                    return;
                }

                callback.onError("Register failed. HTTP code: " + response.code());
            }

            @Override
            public void onFailure(Call<BaseResponse> call, Throwable throwable) {
                callback.onError("Network error: " + throwable.getMessage());
            }
        });
    }

    private void clearTokens() {
        TokenStorage.accessToken = null;
        TokenStorage.refreshToken = null;
        TokenStorage.username = null;
    }
}
