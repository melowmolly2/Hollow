package dto.auction;

import dto.common.BaseResponse;

import java.util.List;

public class SellerListingResponse extends BaseResponse {
    public PageData entity;

    public static class PageData {
        public List<ItemResponse> content;
        public int totalPages;
        public long totalElements;
        public int number;
        public int size;
        public boolean first;
        public boolean last;
    }
}
