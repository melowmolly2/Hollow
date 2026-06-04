package service.account;

import dto.account.DepositRequest;
import dto.account.BalanceResponse;
import network.ApiClient;
import model.TokenStorage;
import service.auth.AuthService;
import service.auth.TokenRefreshCallback;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountService {
    private final AuthService authService = new AuthService();

    public void getBalance(BalanceCallback callback) {
        getBalance(callback, true);
    }

    private void getBalance(BalanceCallback callback, boolean allowRefresh) {
        if (TokenStorage.accessToken == null || TokenStorage.accessToken.isBlank()) {
            if (canRefresh(allowRefresh)) {
                refreshThen(() -> getBalance(callback, false), callback::onError);
                return;
            }
            callback.onError("You must login first");
            return;
        }

        String authorization = "Bearer " + TokenStorage.accessToken;

        ApiClient.api.getBalance(authorization).enqueue(new Callback<BalanceResponse>() {
            @Override
            public void onResponse(Call<BalanceResponse> call, Response<BalanceResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }

                if (isAuthExpired(response) && allowRefresh) {
                    refreshThen(() -> getBalance(callback, false), callback::onError);
                    return;
                }

                callback.onError("Get balance failed. HTTP code: " + response.code());
            }

            @Override
            public void onFailure(Call<BalanceResponse> call, Throwable throwable) {
                callback.onError("Network error: " + throwable.getMessage());
            }
        });
    }

    public void deposit(String amountText, BalanceCallback callback) {
        deposit(amountText, callback, true);
    }

    private void deposit(String amountText, BalanceCallback callback, boolean allowRefresh) {
        if (TokenStorage.accessToken == null || TokenStorage.accessToken.isBlank()) {
            if (canRefresh(allowRefresh)) {
                refreshThen(() -> deposit(amountText, callback, false), callback::onError);
                return;
            }
            callback.onError("You must login first");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            callback.onError("Amount must be a valid number");
            return;
        }

        if (amount <= 0) {
            callback.onError("Amount must be positive");
            return;
        }

        String authorization = "Bearer " + TokenStorage.accessToken;
        DepositRequest request = new DepositRequest(amount);

        ApiClient.api.deposit(authorization, request).enqueue(new Callback<BalanceResponse>() {
            @Override
            public void onResponse(Call<BalanceResponse> call, Response<BalanceResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }

                if (isAuthExpired(response) && allowRefresh) {
                    refreshThen(() -> deposit(amountText, callback, false), callback::onError);
                    return;
                }

                callback.onError("Deposit failed. HTTP code: " + response.code());
            }

            @Override
            public void onFailure(Call<BalanceResponse> call, Throwable throwable) {
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

    private boolean isAuthExpired(Response<?> response) {
        return response.code() == 401 || response.code() == 403;
    }

    private boolean canRefresh(boolean allowRefresh) {
        return allowRefresh && TokenStorage.refreshToken != null && !TokenStorage.refreshToken.isBlank();
    }
}
