package network;

import model.request.LoginRequest;
import model.request.PublishItemRequest;
import model.request.RegisterRequest;
import model.response.AuthResponse;
import model.response.BaseItemResponse;
import model.response.BaseResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface AuctionApi {
    @POST("/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("/register")
    Call<BaseResponse> register(@Body RegisterRequest request);

    @POST("/items")
    Call<BaseItemResponse> createItem(
            @Header("Authorization") String authorization,
            @Body PublishItemRequest request
    );
}
