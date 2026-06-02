package service.auth;

public interface TokenRefreshCallback {
    void onSuccess();

    void onError(String message);
}
