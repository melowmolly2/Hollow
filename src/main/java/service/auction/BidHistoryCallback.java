package service.auction;

import dto.auction.BidHistoryResponse;

public interface BidHistoryCallback {
    void onSuccess(BidHistoryResponse response);

    void onError(String message);
}
