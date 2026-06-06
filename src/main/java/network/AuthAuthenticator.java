package network;

import dto.auth.AuthResponse;
import dto.auth.RefreshTokenRequest;
import model.TokenStorage;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;

import java.io.IOException;

public class AuthAuthenticator {

    private AuthAuthenticator() {
    }

    public static Request authenticate(Response response) throws IOException {
        if (response.code() != 498) {
            return null;
        }

        synchronized (TokenStorage.class) {
            if (!TokenStorage.hasRefreshToken()) {
                TokenStorage.clear();
                return null;
            }

            String currentRefreshToken = TokenStorage.getRefreshToken();
            if (currentRefreshToken == null || currentRefreshToken.isBlank()) {
                TokenStorage.clear();
                return null;
            }

            try {
                Call<AuthResponse> refreshCall = ApiClient.publicApi.refresh(
                        new RefreshTokenRequest(currentRefreshToken));
                retrofit2.Response<AuthResponse> refreshResponse = refreshCall.execute();

                if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                    TokenStorage.setTokens(refreshResponse.body());

                    String newAuthorization = TokenStorage.authorizationHeader();
                    if (newAuthorization == null) {
                        TokenStorage.clear();
                        return null;
                    }

                    return response.request().newBuilder()
                            .removeHeader("Authorization")
                            .addHeader("Authorization", newAuthorization)
                            .build();
                }

                TokenStorage.clear();
                return null;
            } catch (IOException e) {
                TokenStorage.clear();
                return null;
            }
        }
    }
}
