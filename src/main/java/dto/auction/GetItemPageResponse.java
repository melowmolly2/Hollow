package dto.auction;

import dto.common.BaseResponse;

import java.util.List;

public class GetItemPageResponse extends BaseResponse{
    public PageData pages;

    public static class PageData{
        public List<ItemResponse> content;
        public int totalPages;
        public int number;
        public int size;
        public boolean first;
        public boolean last;
    }
}
