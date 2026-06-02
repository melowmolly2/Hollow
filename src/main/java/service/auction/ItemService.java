package service.auction;

import dto.auction.PublishItemRequest;
import dto.auction.BaseItemResponse;
import dto.auction.GetItemPageResponse;
import dto.auction.ItemStatusResponse;
import dto.auction.SellerListingResponse;
import dto.common.BaseResponse;
import network.ApiClient;
import model.TokenStorage;
import service.auth.AuthService;
import service.auth.TokenRefreshCallback;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ItemService {
    private final AuthService authService = new AuthService();

    public void getSellerListings(String username, int page, int size, SellerListingCallback callback) {
        if (username == null || username.isBlank()) {
            callback.onError("Missing username");
            return;
        }

        ApiClient.api.getSellerListings(username, page, size).enqueue(new Callback<SellerListingResponse>() {
            @Override
            public void onResponse(Call<SellerListingResponse> call, Response<SellerListingResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }

                callback.onError("Get seller listings failed. HTTP code: " + response.code());
            }

            @Override
            public void onFailure(Call<SellerListingResponse> call, Throwable throwable) {
                callback.onError("Network error: " + throwable.getMessage());
            }
        });
    }

    public void cancelItem(Long itemId, BaseResponseCallback callback) {
        cancelItem(itemId, callback, true);
    }

    private void cancelItem(Long itemId, BaseResponseCallback callback, boolean allowRefresh) {
        if (TokenStorage.accessToken == null || TokenStorage.accessToken.isBlank()) {
            callback.onError("You must login first");
            return;
        }

        if (itemId == null) {
            callback.onError("Missing item id");
            return;
        }

        String authorization = "Bearer " + TokenStorage.accessToken;
        ApiClient.api.cancelItem(authorization, itemId).enqueue(new Callback<BaseResponse>() {
            @Override
            public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }

                if (response.code() == 401 && allowRefresh) {
                    refreshThen(() -> cancelItem(itemId, callback, false), callback::onError);
                    return;
                }

                callback.onError("Cancel item failed. HTTP code: " + response.code());
            }

            @Override
            public void onFailure(Call<BaseResponse> call, Throwable throwable) {
                callback.onError("Network error: " + throwable.getMessage());
            }
        });
    }

    public void createItem(
            String title,
            String description,
            String durationMinutesText,
            String startingPriceText,
            String bidIncrementText,
            String buyItNowPriceText,
            ItemCallback callback
    ) {
        createItem(title, description, durationMinutesText, startingPriceText, bidIncrementText, buyItNowPriceText, callback, true);
    }

    private void createItem(
            String title,
            String description,
            String durationMinutesText,
            String startingPriceText,
            String bidIncrementText,
            String buyItNowPriceText,
            ItemCallback callback,
            boolean allowRefresh
    ) {
        if (TokenStorage.accessToken == null || TokenStorage.accessToken.isBlank()) {
            callback.onError("You must login first");
            return;
        }
        if (title == null || title.isBlank()) {
            callback.onError("Title is empty");
            return;
        }

        if (description == null) {
            callback.onError("Description is empty");
            return;
        }

        long durationMinutes;
        double startingPrice;
        double bidIncrement;
        double buyItNowPrice;

        try {
            durationMinutes = Long.parseLong(durationMinutesText);
            startingPrice = Double.parseDouble(startingPriceText);
            bidIncrement = Double.parseDouble(bidIncrementText);
            buyItNowPrice = Double.parseDouble(buyItNowPriceText);
        } catch (NumberFormatException e) {
            callback.onError("Time and prices must be valid numbers");
            return;
        }

        if (durationMinutes <= 0) {
            callback.onError("Duration must be positive");
            return;
        }

        if (startingPrice <= 0 || bidIncrement <= 0 || buyItNowPrice <= 0) {
            callback.onError("Prices must be positive");
            return;
        }

        long endTime = System.currentTimeMillis() + durationMinutes * 60_000;

        PublishItemRequest request = new PublishItemRequest(
                title,
                description,
                endTime,
                startingPrice,
                buyItNowPrice,
                bidIncrement
        );

        String authorization = "Bearer " + TokenStorage.accessToken;

        ApiClient.api.createItem(authorization, request).enqueue(new Callback<BaseItemResponse>() {
            @Override
            public void onResponse(Call<BaseItemResponse> call, Response<BaseItemResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }

                if (response.code() == 401 && allowRefresh) {
                    refreshThen(
                            () -> createItem(title, description, durationMinutesText, startingPriceText,
                                    bidIncrementText, buyItNowPriceText, callback, false),
                            callback::onError
                    );
                    return;
                }

                callback.onError("Create item failed. HTTP code: " + response.code());
            }

            @Override
            public void onFailure(Call<BaseItemResponse> call, Throwable throwable) {
                callback.onError("Network error: " + throwable.getMessage());
            }
        });
    }

    private void refreshThen(Runnable onSuccess, java.util.function.Consumer<String> onError) {
        authService.refreshToken(new TokenRefreshCallback() {
            @Override
            public void onSuccess() {
                onSuccess.run();
            }

            @Override
            public void onError(String message) {
                onError.accept(message);
            }
        });
    }

    public void getItems(int page, int size, ItemPageCallback callback) {
        ApiClient.api.getItems(page, size).enqueue(new Callback<GetItemPageResponse>() {
            @Override
            public void onResponse(Call<GetItemPageResponse> call, Response<GetItemPageResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }

                callback.onError("Get items failed. HTTP code: " + response.code());
            }

            @Override
            public void onFailure(Call<GetItemPageResponse> call, Throwable throwable) {
                callback.onError("Network error: " + throwable.getMessage());
            }
        });
    }

    public void getItemStatus(Long itemId, ItemStatusCallback callback) {
        if (itemId == null) {
            callback.onError("Missing item id");
            return;
        }

        ApiClient.api.getItemStatus(itemId).enqueue(new Callback<ItemStatusResponse>() {
            @Override
            public void onResponse(Call<ItemStatusResponse> call, Response<ItemStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }

                callback.onError("Get item status failed. HTTP code: " + response.code());
            }

            @Override
            public void onFailure(Call<ItemStatusResponse> call, Throwable throwable) {
                callback.onError("Network error: " + throwable.getMessage());
            }
        });
    }
}
