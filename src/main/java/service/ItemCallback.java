package service;

import model.response.BaseItemResponse;

public interface ItemCallback {
    void onSuccess(BaseItemResponse response);

    void onError(String message);
}