package dto.auction;

import com.google.gson.annotations.SerializedName;
import dto.admin.UserResponse;

public class ItemResponse {
    public Long itemId;
    public String title;
    public String description;
    @SerializedName(value = "sellerUsername", alternate = {"seller_username", "username"})
    public String sellerUsername;
    public UserResponse user;

    public ItemResponse() {
    }
}
