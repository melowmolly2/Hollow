package dto.admin;

import com.google.gson.annotations.SerializedName;
import dto.common.BaseResponse;

import java.util.List;

public class UserListResponse extends BaseResponse {
    @SerializedName("entity")
    public List<UserResponse> users;

    public UserListResponse() {
    }
}
