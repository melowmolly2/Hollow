package dto.auction;

import com.google.gson.annotations.SerializedName;
import dto.common.BaseResponse;

import java.util.List;

public class ItemListResponse extends BaseResponse {
    @SerializedName("entity")
    public List<ItemResponse> items;

    public ItemListResponse() {
    }
}
