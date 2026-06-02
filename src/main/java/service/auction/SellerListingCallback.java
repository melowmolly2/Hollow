package service.auction;

import dto.auction.SellerListingResponse;

public interface SellerListingCallback {
    void onSuccess(SellerListingResponse response);

    void onError(String message);
}
