package service.auction;

import dto.auction.MyWinsResponse;

public interface MyWinsCallback {
    void onSuccess(MyWinsResponse response);

    void onError(String message);
}
