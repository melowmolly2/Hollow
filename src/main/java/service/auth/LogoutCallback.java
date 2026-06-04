package service.auth;

public interface LogoutCallback {
    void onSuccess(String message);

    void onError(String message);
}
