package dto.auction;

import com.google.gson.annotations.SerializedName;
import dto.common.BaseResponse;

public class BidPostResponse extends BaseResponse {
    @SerializedName("entity")
    public BidResponse bid;

    public BidPostResponse() {
    }

    public static class BidResponse {
        public Long bidId;
        public Double bidAmount;
        public Long time;
    }
}
