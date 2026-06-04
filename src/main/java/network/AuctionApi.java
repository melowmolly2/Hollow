package network;

import dto.auction.BidPostRequest;
import dto.auction.BaseItemResponse;
import dto.auction.BidPostResponse;
import dto.auction.BidHistoryResponse;
import dto.auction.GetItemPageResponse;
import dto.auction.ItemStatusResponse;
import dto.auction.SellerListingResponse;
import dto.account.DepositRequest;
import dto.account.BalanceResponse;
import dto.auth.AuthResponse;
import dto.auth.LoginRequest;
import dto.auth.RefreshTokenRequest;
import dto.auction.PublishItemRequest;
import dto.auth.RegisterRequest;
import dto.common.BaseResponse;
import retrofit2.Call;
import retrofit2.http.*;

public interface AuctionApi {
    @POST("/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("/register")
    Call<BaseResponse> register(@Body RegisterRequest request);

    @POST("/refresh")
    Call<AuthResponse> refresh(@Body RefreshTokenRequest request);

    @POST("/items")
    Call<BaseItemResponse> createItem(
            @Header("Authorization") String authorization,
            @Body PublishItemRequest request
    );

    @GET("/items")
    Call<GetItemPageResponse> getItems(@Query("page") int page,
                                       @Query("size") int size);

    @GET("/items/listings/{username}")
    Call<SellerListingResponse> getSellerListings(@Path("username") String username,
                                                  @Query("page") int page,
                                                  @Query("size") int size);

    @GET("/item/status/{itemId}")
    Call<ItemStatusResponse> getItemStatus(@Path("itemId") Long itemId);

    @GET("/users/me/balance")
    Call<BalanceResponse> getBalance(@Header("Authorization") String authorization);

    @POST("/users/me/deposit")
    Call<BalanceResponse> deposit(
            @Header("Authorization") String authorization,
            @Body DepositRequest request
    );

    @POST("/bid")
    Call<BidPostResponse> placeBid(
            @Header("Authorization") String authorization,
            @Body BidPostRequest request
    );

    @GET("/bids/{itemId}/bids")
    Call<BidHistoryResponse> getBidHistory(@Path("itemId") Long itemId,
                                           @Query("page") int page,
                                           @Query("size") int size);

    @POST("/items/cancel/{itemId}")
    Call<BaseResponse> cancelItem(@Header("Authorization") String authorization,
                                  @Path("itemId") Long itemId);
}
