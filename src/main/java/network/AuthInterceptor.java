package network;

import model.TokenStorage;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class AuthInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        if (!requiresAuthHandling(request)) {
            return chain.proceed(request);
        }

        Request.Builder requestBuilder = request.newBuilder()
                .removeHeader("Authorization");

        String authorization = TokenStorage.authorizationHeader();
        if (authorization == null) {
            return chain.proceed(requestBuilder.build());
        }

        Request authenticatedRequest = requestBuilder
                .addHeader("Authorization", authorization)
                .build();

        return chain.proceed(authenticatedRequest);
    }

    private boolean requiresAuthHandling(Request request) {
        String path = request.url().encodedPath();
        return !"/login".equals(path)
                && !"/register".equals(path)
                && !"/refresh".equals(path);
    }
}
