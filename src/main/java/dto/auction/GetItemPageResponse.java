package dto.auction;

import com.google.gson.annotations.SerializedName;
import dto.common.BaseResponse;

import java.util.List;

public class GetItemPageResponse extends BaseResponse{
    @SerializedName("entity")
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
