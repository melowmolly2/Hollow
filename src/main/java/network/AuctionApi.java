package network;

import dto.auction.BidPostRequest;
import dto.auction.AutoBidRequest;
import dto.auction.BaseItemResponse;
import dto.auction.BidPostResponse;
import dto.auction.BidHistoryResponse;
import dto.auction.GetItemPageResponse;
import dto.auction.ItemListResponse;
import dto.auction.ItemStatusResponse;
import dto.auction.MyWinsResponse;
import dto.auction.SellerListingResponse;
import dto.account.DepositRequest;
import dto.account.BalanceResponse;
import dto.admin.BanUserRequest;
import dto.admin.UnbanUserRequest;
import dto.admin.UserListResponse;
import dto.auth.AuthResponse;
import dto.auth.LoginRequest;
import dto.auth.RefreshTokenRequest;
import dto.auction.PublishItemRequest;
import dto.auth.RegisterRequest;
import dto.common.BaseResponse;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface AuctionApi {
    @POST("/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("/register")
    Call<BaseResponse> register(@Body RegisterRequest request);

    @POST("/refresh")
    Call<AuthResponse> refresh(@Body RefreshTokenRequest request);

    @POST("/logout")
    Call<BaseResponse> logout(@Header("Authorization") String authorization);

    @POST("/items")
    Call<BaseItemResponse> createItem(
            @Header("Authorization") String authorization,
            @Body PublishItemRequest request
    );

    @GET("/items")
    Call<GetItemPageResponse> getItems(@Query("page") int page,
                                       @Query("size") int size);

    @GET("/items/all")
    Call<ItemListResponse> getAllItems();

    @GET("/items/listings/{username}")
    Call<SellerListingResponse> getSellerListings(@Path("username") String username,
                                                  @Query("page") int page,
                                                  @Query("size") int size);

    @GET("/item/status/{itemId}")
    Call<ItemStatusResponse> getItemStatus(@Path("itemId") Long itemId);

    @GET("/item/status/all")
    Call<List<ItemStatusResponse.ItemStatusData>> getAllItemStatuses();

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

    @POST("/auto-bid")
    Call<BaseResponse> autoBid(
            @Header("Authorization") String authorization,
            @Body AutoBidRequest request
    );

    @GET("/bids/{itemId}/bids")
    Call<BidHistoryResponse> getBidHistory(@Path("itemId") Long itemId,
                                           @Query("page") int page,
                                           @Query("size") int size);

    @GET("/me/wins")
    Call<MyWinsResponse> getMyWins();

    @POST("/items/cancel/{itemId}")
    Call<BaseResponse> cancelItem(@Header("Authorization") String authorization,
                                  @Path("itemId") Long itemId);

    @POST("/buy-now/{itemId}")
    Call<BaseResponse> buyNow(@Header("Authorization") String authorization,
                              @Path("itemId") Long itemId);

    @POST("/admin/ban")
    Call<BaseResponse> banUser(
            @Header("Authorization") String authorization,
            @Body BanUserRequest request
    );

    @POST("/admin/unban")
    Call<BaseResponse> unbanUser(
            @Header("Authorization") String authorization,
            @Body UnbanUserRequest request
    );

    @POST("/admin/cancel/{itemId}")
    Call<BaseResponse> adminCancelItem(
            @Header("Authorization") String authorization,
            @Path("itemId") Long itemId
    );

    @GET("/admin/users")
    Call<UserListResponse> getUsers(@Header("Authorization") String authorization);
}
