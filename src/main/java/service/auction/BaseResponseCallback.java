package service.auction;

import dto.common.BaseResponse;

public interface BaseResponseCallback {
    void onSuccess(BaseResponse response);

    void onError(String message);
}
