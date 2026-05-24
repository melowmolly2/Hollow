package service;

import model.request.LoginRequest;
import model.request.RegisterRequest;
import model.response.AuthResponse;
import model.response.BaseResponse;
import network.ApiClient;
import network.TokenStorage;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthService {
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
}
