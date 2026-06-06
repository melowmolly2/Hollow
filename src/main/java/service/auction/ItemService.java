package service.auction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dto.auction.PublishItemRequest;
import dto.auction.BaseItemResponse;
import dto.auction.GetItemPageResponse;
import dto.auction.ItemStatusResponse;
import dto.auction.SellerListingResponse;
import dto.common.BaseResponse;
import network.ApiClient;
import model.TokenStorage;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;

public class ItemService {
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

                callback.onError(errorMessage(response, "Get seller listings failed"));
            }

            @Override
            public void onFailure(Call<SellerListingResponse> call, Throwable throwable) {
                callback.onError("Network error: " + throwable.getMessage());
            }
        });
    }

    public void cancelItem(Long itemId, BaseResponseCallback callback) {
        if (!TokenStorage.hasAccessToken()) {
            callback.onError("You must login first");
            return;
        }

        if (itemId == null) {
            callback.onError("Missing item id");
            return;
        }

        ApiClient.api.cancelItem(itemId).enqueue(new Callback<BaseResponse>() {
            @Override
            public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }

                if (isAuthExpired(response)) {
                    callback.onError("Session expired. Please login again.");
                    return;
                }

                callback.onError(errorMessage(response, "Cancel item failed"));
            }

            @Override
            public void onFailure(Call<BaseResponse> call, Throwable throwable) {
                callback.onError("Network error: " + throwable.getMessage());
            }
        });
    }

    public void buyNow(Long itemId, BaseResponseCallback callback) {
        if (!TokenStorage.hasAccessToken()) {
            callback.onError("You must login first");
            return;
        }

        if (itemId == null) {
            callback.onError("Missing item id");
            return;
        }

        ApiClient.api.buyNow(itemId).enqueue(new Callback<BaseResponse>() {
            @Override
            public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }

                if (isAuthExpired(response)) {
                    callback.onError("Session expired. Please login again.");
                    return;
                }

                callback.onError(errorMessage(response, "Buy it now failed"));
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
        if (!TokenStorage.hasAccessToken()) {
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

        ApiClient.api.createItem(request).enqueue(new Callback<BaseItemResponse>() {
            @Override
            public void onResponse(Call<BaseItemResponse> call, Response<BaseItemResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }

                if (isAuthExpired(response)) {
                    callback.onError("Session expired. Please login again.");
                    return;
                }

                callback.onError(errorMessage(response, "Create item failed"));
            }

            @Override
            public void onFailure(Call<BaseItemResponse> call, Throwable throwable) {
                callback.onError("Network error: " + throwable.getMessage());
            }
        });
    }

    private boolean isAuthExpired(Response<?> response) {
        return response.code() == 401 || response.code() == 403 || response.code() == 498;
    }

    public void getItems(int page, int size, ItemPageCallback callback) {
        ApiClient.api.getItems(page, size).enqueue(new Callback<GetItemPageResponse>() {
            @Override
            public void onResponse(Call<GetItemPageResponse> call, Response<GetItemPageResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }

                callback.onError(errorMessage(response, "Get items failed"));
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

                callback.onError(errorMessage(response, "Get item status failed"));
            }

            @Override
            public void onFailure(Call<ItemStatusResponse> call, Throwable throwable) {
                callback.onError("Network error: " + throwable.getMessage());
            }
        });
    }

    private String errorMessage(Response<?> response, String fallback) {
        String message = readMessage(response.errorBody());
        if (message != null && !message.isBlank()) {
            return message;
        }

        return fallback + ". HTTP code: " + response.code();
    }

    private String readMessage(ResponseBody errorBody) {
        if (errorBody == null) {
            return null;
        }

        try {
            JsonObject json = JsonParser.parseString(errorBody.string()).getAsJsonObject();
            if (json.has("message") && !json.get("message").isJsonNull()) {
                return json.get("message").getAsString();
            }
        } catch (IOException | IllegalStateException ignored) {
            return null;
        }

        return null;
    }
}
