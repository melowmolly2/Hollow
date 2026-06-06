package network;

import dto.auth.AuthResponse;
import dto.auth.RefreshTokenRequest;
import model.TokenStorage;
import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import service.auth.TokenRefreshManager;
import retrofit2.Call;

import java.io.IOException;

public class AuthAuthenticator implements Authenticator {

    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        if (response.code() != 498) {
            return null;
        }

        synchronized (TokenStorage.class) {
            if (!TokenStorage.hasRefreshToken()) {
                TokenStorage.clear();
                TokenRefreshManager.stop();
                return null;
            }

            String currentRefreshToken = TokenStorage.getRefreshToken();
            if (currentRefreshToken == null || currentRefreshToken.isBlank()) {
                TokenStorage.clear();
                TokenRefreshManager.stop();
                return null;
            }

            try {
                Call<AuthResponse> refreshCall = ApiClient.publicApi.refresh(
                        new RefreshTokenRequest(currentRefreshToken));
                retrofit2.Response<AuthResponse> refreshResponse = refreshCall.execute();

                if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                    TokenStorage.setTokens(refreshResponse.body());
                    TokenRefreshManager.start();

                    String newAuthorization = TokenStorage.authorizationHeader();
                    if (newAuthorization == null) {
                        TokenStorage.clear();
                        TokenRefreshManager.stop();
                        return null;
                    }

                    return response.request().newBuilder()
                            .removeHeader("Authorization")
                            .addHeader("Authorization", newAuthorization)
                            .build();
                }

                TokenStorage.clear();
                TokenRefreshManager.stop();
                return null;
            } catch (IOException e) {
                TokenStorage.clear();
                TokenRefreshManager.stop();
                return null;
            }
        }
    }
}
