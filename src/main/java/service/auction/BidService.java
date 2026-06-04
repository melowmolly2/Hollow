package service.auction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dto.auction.AutoBidRequest;
import dto.auction.BidPostRequest;
import dto.auction.BidPostResponse;
import dto.auction.BidHistoryResponse;
import dto.auction.MyWinsResponse;
import dto.common.BaseResponse;
import model.TokenStorage;
import network.ApiClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;

public class BidService {
    public void getMyWins(MyWinsCallback callback) {
        ApiClient.api.getMyWins().enqueue(new Callback<MyWinsResponse>() {
            @Override
            public void onResponse(Call<MyWinsResponse> call, Response<MyWinsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }

                callback.onError(errorMessage(response, "Get winning history failed"));
            }

            @Override
            public void onFailure(Call<MyWinsResponse> call, Throwable throwable) {
                callback.onError("Network error: " + throwable.getMessage());
            }
        });
    }

    public void getBidHistory(Long itemId, int page, int size, BidHistoryCallback callback) {
        if (itemId == null) {
            callback.onError("Missing item id");
            return;
        }

        ApiClient.api.getBidHistory(itemId, page, size).enqueue(new Callback<BidHistoryResponse>() {
            @Override
            public void onResponse(Call<BidHistoryResponse> call, Response<BidHistoryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }

                callback.onError(errorMessage(response, "Get bid history failed"));
            }

            @Override
            public void onFailure(Call<BidHistoryResponse> call, Throwable throwable) {
                callback.onError("Network error: " + throwable.getMessage());
            }
        });
    }

    public void placeBid(Long itemId, String bidAmountText, BidCallback callback) {
        if (TokenStorage.accessToken == null || TokenStorage.accessToken.isBlank()) {
            callback.onError("You must login first");
            return;
        }

        if (itemId == null) {
            callback.onError("Missing item id");
            return;
        }

        double bidAmount;
        try {
            bidAmount = Double.parseDouble(bidAmountText);
        } catch (NumberFormatException e) {
            callback.onError("Bid amount must be a valid number");
            return;
        }

        if (bidAmount <= 0) {
            callback.onError("Bid amount must be positive");
            return;
        }

        String authorization = "Bearer " + TokenStorage.accessToken;
        BidPostRequest request = new BidPostRequest(itemId, bidAmount);

        ApiClient.api.placeBid(authorization, request).enqueue(new Callback<BidPostResponse>() {
            @Override
            public void onResponse(Call<BidPostResponse> call, Response<BidPostResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }

                if (isAuthExpired(response)) {
                    callback.onError("Session expired. Please login again.");
                    return;
                }

                callback.onError(errorMessage(response, "Bid failed"));
            }

            @Override
            public void onFailure(Call<BidPostResponse> call, Throwable throwable) {
                callback.onError("Network error: " + throwable.getMessage());
            }
        });
    }

    public void autoBid(Long itemId, String maxBidLimitText, BaseResponseCallback callback) {
        if (TokenStorage.accessToken == null || TokenStorage.accessToken.isBlank()) {
            callback.onError("You must login first");
            return;
        }

        if (itemId == null) {
            callback.onError("Missing item id");
            return;
        }

        double maxBidLimit;
        try {
            maxBidLimit = Double.parseDouble(maxBidLimitText);
        } catch (NumberFormatException e) {
            callback.onError("Max bid limit must be a valid number");
            return;
        }

        if (maxBidLimit <= 0) {
            callback.onError("Max bid limit must be positive");
            return;
        }

        String authorization = "Bearer " + TokenStorage.accessToken;
        AutoBidRequest request = new AutoBidRequest(itemId, maxBidLimit);

        ApiClient.api.autoBid(authorization, request).enqueue(new Callback<BaseResponse>() {
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

                callback.onError(errorMessage(response, "Auto bid failed"));
            }

            @Override
            public void onFailure(Call<BaseResponse> call, Throwable throwable) {
                callback.onError("Network error: " + throwable.getMessage());
            }
        });
    }

    private boolean isAuthExpired(Response<?> response) {
        return response.code() == 401 || response.code() == 403;
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
