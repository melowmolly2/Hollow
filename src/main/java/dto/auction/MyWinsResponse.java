package dto.auction;

import dto.common.BaseResponse;

import java.util.List;

public class MyWinsResponse extends BaseResponse {
    public List<WinData> entity;

    public static class WinData {
        public BidHistoryResponse.BidData bid;
        public ItemResponse item;
    }
}
