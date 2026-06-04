package dto.auction;

import dto.common.BaseResponse;

public class ItemStatusResponse extends BaseResponse {
    public ItemStatusData itemStatus;
    public static class ItemStatusData{
        public long id;
        public Double currentPrice;
        public String highestBidUser;
        public Long startTime;
        public Long endTime;
        public Long maxEndTime;
        public Double startingPrice;
        public Double buyItNowPrice;
        public Double bidIncrement;
        public String itemStatus;
    }
}
