package com.kronos.olympusfront.network;

import android.content.Context;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String BASE_URL = "http://46.224.227.209:8081/"; // 10.0.2.2 for Android Emulator connecting to localhost
    private static Retrofit retrofit = null;
    private static ApiService apiService = null;

    public static void init(Context context) {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor(context))
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            apiService = retrofit.create(ApiService.class);
        }
    }

    public static ApiService getApiService() {
        if (apiService == null) {
            // Unsafe fallback if init was not called, but avoid crashing if possible
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            apiService = retrofit.create(ApiService.class);
        }
        return apiService;
    }
}