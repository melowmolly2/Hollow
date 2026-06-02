package dto.auction;

import dto.common.BaseResponse;

import java.util.List;

public class BidHistoryResponse extends BaseResponse {
    public PageData entity;

    public static class PageData {
        public List<BidData> content;
        public int totalPages;
        public int number;
        public int size;
        public boolean first;
        public boolean last;
    }

    public static class BidData {
        public Long bidId;
        public Double bidAmount;
        public Long time;
    }
}
