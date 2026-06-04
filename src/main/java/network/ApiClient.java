package network;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    public static final String BASE_URL = "http://26.106.51.93:8080/";

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
}
