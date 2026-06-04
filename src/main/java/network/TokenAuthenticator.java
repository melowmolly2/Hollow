package network;

import model.TokenStorage;
import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import service.auth.TokenRefreshManager;

public class TokenAuthenticator implements Authenticator {
    @Override
    public Request authenticate(Route route, Response response) {
        if (responseCount(response) > 1 || !requiresAuthHandling(response.request())) {
            return null;
        }

        String requestAuthorization = response.request().header("Authorization");
        String currentAuthorization = TokenStorage.authorizationHeader();
        if (currentAuthorization != null && !currentAuthorization.equals(requestAuthorization)) {
            return retryWith(currentAuthorization, response.request());
        }

        if (!TokenRefreshManager.forceRefreshBlocking()) {
            return null;
        }

        String newAuthorization = TokenStorage.authorizationHeader();
        if (newAuthorization == null || newAuthorization.equals(requestAuthorization)) {
            return null;
        }

        return retryWith(newAuthorization, response.request());
    }

    private Request retryWith(String authorization, Request request) {
        return request.newBuilder()
                .removeHeader("Authorization")
                .addHeader("Authorization", authorization)
                .build();
    }

    private int responseCount(Response response) {
        int count = 1;
        Response priorResponse = response.priorResponse();
        while (priorResponse != null) {
            count++;
            priorResponse = priorResponse.priorResponse();
        }
        return count;
    }

    private boolean requiresAuthHandling(Request request) {
        String path = request.url().encodedPath();
        return !"/login".equals(path)
                && !"/register".equals(path)
                && !"/refresh".equals(path);
    }
}
