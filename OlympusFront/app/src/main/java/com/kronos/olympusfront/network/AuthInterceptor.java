package com.kronos.olympusfront.network;

import android.content.Context;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    private TokenManager tokenManager;

    public AuthInterceptor(Context context) {
        this.tokenManager = new TokenManager(context);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request.Builder requestBuilder = chain.request().newBuilder();
        String token = tokenManager.getToken();
        if (token != null && !token.isEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer " + token);
        }
        return chain.proceed(requestBuilder.build());
    }
}