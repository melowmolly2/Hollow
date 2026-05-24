package service;

import model.response.AuthResponse;

public interface LoginCallback {
    void onSuccess(AuthResponse response);

    void onError(String message);
}
