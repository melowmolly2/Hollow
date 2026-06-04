package dto.account;

import com.google.gson.annotations.SerializedName;
import dto.common.BaseResponse;

public class BalanceResponse extends BaseResponse {
    @SerializedName("entity")
    public Double balance;

    public BalanceResponse() {
    }
}
