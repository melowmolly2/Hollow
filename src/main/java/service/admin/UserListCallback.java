package service.admin;

import dto.admin.UserListResponse;

public interface UserListCallback {
    void onSuccess(UserListResponse response);

    void onError(String message);
}
