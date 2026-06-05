package service.admin;

import dto.auction.ItemListResponse;

public interface ItemListCallback {
    void onSuccess(ItemListResponse response);

    void onError(String message);
}
