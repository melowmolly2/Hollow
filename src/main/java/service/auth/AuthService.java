package service.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dto.auth.LoginRequest;
import dto.auth.RegisterRequest;
import dto.auth.AuthResponse;
import dto.common.BaseResponse;
import network.ApiClient;
import model.TokenStorage;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;

public class AuthService {
    public void logout(LogoutCallback callback) {
        if (!TokenStorage.hasAccessToken() && !TokenStorage.hasRefreshToken()) {
            TokenStorage.clear();
            callback.onSuccess("Logged out.");
            return;
        }

        ApiClient.api.logout().enqueue(new Callback<BaseResponse>() {
            @Override
            public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String message = response.body().message;
                    TokenStorage.clear();
                    callback.onSuccess(message == null || message.isBlank() ? "Logged out." : message);
                    return;
                }

                TokenStorage.clear();
                callback.onSuccess("Logged out.");
            }

            @Override
            public void onFailure(Call<BaseResponse> call, Throwable throwable) {
                TokenStorage.clear();
                callback.onSuccess("Logged out.");
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
        ApiClient.publicApi.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse auth = response.body();
                    TokenStorage.setSession(username, auth);
                    callback.onSuccess(auth);
                    return;
                }

                callback.onError(errorMessage(response, "Login failed"));
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
        ApiClient.publicApi.register(request).enqueue(new Callback<BaseResponse>() {
            @Override
            public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().message);
                    return;
                }

                callback.onError(errorMessage(response, "Register failed"));
            }

            @Override
            public void onFailure(Call<BaseResponse> call, Throwable throwable) {
                callback.onError("Network error: " + throwable.getMessage());
            }
        });
    }

    private String errorMessage(Response<?> response, String fallback) {
        String message = readMessage(response.errorBody());
        if (message != null && !message.isBlank()) {
            return message;
        }

        return fallback + ". HTTP code: " + response.code();
    }

    private String readMessage(ResponseBody errorBody) {
        if (errorBody == null) {
            return null;
        }

        try {
            JsonObject json = JsonParser.parseString(errorBody.string()).getAsJsonObject();
            if (json.has("message") && !json.get("message").isJsonNull()) {
                return json.get("message").getAsString();
            }
            for (String key : json.keySet()) {
                if (!json.get(key).isJsonNull()) {
                    return json.get(key).getAsString();
                }
            }
        } catch (IOException | IllegalStateException ignored) {
            return null;
        }

        return null;
    }
}
