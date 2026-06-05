package service.admin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dto.admin.BanUserRequest;
import dto.admin.UnbanUserRequest;
import dto.admin.UserListResponse;
import dto.auction.GetItemPageResponse;
import dto.auction.ItemListResponse;
import dto.auction.ItemResponse;
import dto.auction.ItemStatusResponse;
import dto.common.BaseResponse;
import model.TokenStorage;
import network.ApiClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import service.auction.BaseResponseCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminService {
    public void banUser(String username, BaseResponseCallback callback) {
        String authorization = requireAuthorization(callback);
        if (authorization == null) {
            return;
        }

        ApiClient.api.banUser(authorization, new BanUserRequest(username)).enqueue(baseCallback(callback, "Ban user failed"));
    }

    public void unbanUser(String username, String password, BaseResponseCallback callback) {
        String authorization = requireAuthorization(callback);
        if (authorization == null) {
            return;
        }

        ApiClient.api.unbanUser(authorization, new UnbanUserRequest(username, password))
                .enqueue(baseCallback(callback, "Unban user failed"));
    }

    public void endAuction(Long itemId, BaseResponseCallback callback) {
        String authorization = requireAuthorization(callback);
        if (authorization == null) {
            return;
        }

        ApiClient.api.adminCancelItem(authorization, itemId).enqueue(baseCallback(callback, "End auction failed"));
    }

    public void getUsers(UserListCallback callback) {
        String authorization = TokenStorage.authorizationHeader();
        if (authorization == null) {
            callback.onError("You must login first");
            return;
        }

        ApiClient.api.getUsers(authorization).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<UserListResponse> call, Response<UserListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }

                callback.onError(errorMessage(response, "Get users failed"));
            }

            @Override
            public void onFailure(Call<UserListResponse> call, Throwable throwable) {
                callback.onError("Network error: " + throwable.getMessage());
            }
        });
    }

    public void getItems(ItemListCallback callback) {
        loadActiveItemPage(0, new ArrayList<>(), callback);
    }

    private void loadActiveItemPage(int page, List<ItemResponse> items, ItemListCallback callback) {
        ApiClient.api.getItems(page, 20).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<GetItemPageResponse> call, Response<GetItemPageResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    GetItemPageResponse body = response.body();
                    if (body.pages != null && body.pages.content != null) {
                        items.addAll(body.pages.content);
                    }

                    boolean lastPage = body.pages == null || body.pages.last || page + 1 >= body.pages.totalPages;
                    if (lastPage) {
                        completeItemsWithSellerFallback(items, callback);
                        return;
                    }

                    loadActiveItemPage(page + 1, items, callback);
                    return;
                }

                callback.onError(errorMessage(response, "Get active auctions failed"));
            }

            @Override
            public void onFailure(Call<GetItemPageResponse> call, Throwable throwable) {
                callback.onError("Network error: " + throwable.getMessage());
            }
        });
    }

    private void completeItemsWithSellerFallback(List<ItemResponse> items, ItemListCallback callback) {
        if (items.stream().allMatch(item -> item.sellerUsername != null && !item.sellerUsername.isBlank())) {
            callback.onSuccess(itemListResponse(items));
            return;
        }

        ApiClient.api.getAllItemStatuses().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<ItemStatusResponse.ItemStatusData>> call,
                                   Response<List<ItemStatusResponse.ItemStatusData>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<Long, String> usernamesByItemId = new HashMap<>();
                    for (ItemStatusResponse.ItemStatusData status : response.body()) {
                        if (status != null && status.highestBidUser != null && !status.highestBidUser.isBlank()) {
                            usernamesByItemId.put(status.id, status.highestBidUser);
                        }
                    }

                    for (ItemResponse item : items) {
                        if (item != null
                                && item.itemId != null
                                && (item.sellerUsername == null || item.sellerUsername.isBlank())) {
                            item.sellerUsername = usernamesByItemId.get(item.itemId);
                        }
                    }
                }

                callback.onSuccess(itemListResponse(items));
            }

            @Override
            public void onFailure(Call<List<ItemStatusResponse.ItemStatusData>> call, Throwable throwable) {
                callback.onSuccess(itemListResponse(items));
            }
        });
    }

    private ItemListResponse itemListResponse(List<ItemResponse> items) {
        ItemListResponse listResponse = new ItemListResponse();
        listResponse.status = true;
        listResponse.message = "Successfully loaded active auctions";
        listResponse.items = items;
        return listResponse;
    }

    private String requireAuthorization(BaseResponseCallback callback) {
        String authorization = TokenStorage.authorizationHeader();
        if (authorization == null) {
            callback.onError("You must login first");
            return null;
        }
        return authorization;
    }

    private Callback<BaseResponse> baseCallback(BaseResponseCallback callback, String fallback) {
        return new Callback<>() {
            @Override
            public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }

                callback.onError(errorMessage(response, fallback));
            }

            @Override
            public void onFailure(Call<BaseResponse> call, Throwable throwable) {
                callback.onError("Network error: " + throwable.getMessage());
            }
        };
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
            for (String key : json.keySet()) {
                if (!json.get(key).isJsonNull()) {
                    return json.get(key).getAsString();
                }
            }
        } catch (IOException | IllegalStateException ignored) {
            return null;
        }

        return null;
    }
}
