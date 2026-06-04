package dto.auction;

import dto.common.BaseResponse;

public class BidPostResponse extends BaseResponse {
    public BidResponse bid;

    public BidPostResponse() {
    }

    public static class BidResponse {
        public Long bidId;
        public Double bidAmount;
        public Long time;
    }
}
