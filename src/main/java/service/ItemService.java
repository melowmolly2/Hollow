package service;

import model.request.PublishItemRequest;
import model.response.BaseItemResponse;
import network.ApiClient;
import network.TokenStorage;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ItemService {
    public void createItem(
            String title,
            String description,
            String durationMinutesText,
            String startingPriceText,
            String bidIncrementText,
            String buyItNowPriceText,
            ItemCallback callback
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

                callback.onError("Create item failed. HTTP code: " + response.code());
            }

            @Override
            public void onFailure(Call<BaseItemResponse> call, Throwable throwable) {
                callback.onError("Network error: " + throwable.getMessage());
            }
        });
    }
}