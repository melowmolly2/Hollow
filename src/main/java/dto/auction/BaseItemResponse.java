package dto.auction;

import com.google.gson.annotations.SerializedName;
import dto.common.BaseResponse;

public class BaseItemResponse extends BaseResponse {
    @SerializedName("entity")
    public ItemResponse item;

    public BaseItemResponse() {
    }
}
