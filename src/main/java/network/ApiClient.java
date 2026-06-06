package network;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String DEFAULT_BASE_URL = "http://localhost:8080/";
    public static final String BASE_URL = normalizeBaseUrl(resolveBaseUrl());

    private static final OkHttpClient PUBLIC_CLIENT = new OkHttpClient.Builder()
            .build();

    private static final OkHttpClient AUTHENTICATED_CLIENT = new OkHttpClient.Builder()
            .addInterceptor(new AuthInterceptor())
            .build();

    public static final AuctionApi publicApi = retrofit(PUBLIC_CLIENT)
            .create(AuctionApi.class);

    public static final AuctionApi api = retrofit(AUTHENTICATED_CLIENT)
            .create(AuctionApi.class);

    private static Retrofit retrofit(OkHttpClient client) {
        return new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
    }

    private static String resolveBaseUrl() {
        String propertyValue = System.getProperty("auction.apiBaseUrl");
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }

        String environmentValue = System.getenv("AUCTION_API_BASE_URL");
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }

        return DEFAULT_BASE_URL;
    }

    private static String normalizeBaseUrl(String baseUrl) {
        String trimmed = baseUrl.trim();
        return trimmed.endsWith("/") ? trimmed : trimmed + "/";
    }
}
