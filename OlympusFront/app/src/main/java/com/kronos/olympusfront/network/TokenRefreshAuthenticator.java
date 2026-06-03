package com.kronos.olympusfront.network;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.kronos.olympusfront.LoginActivity;
import com.kronos.olympusfront.network.dto.AuthResponse;
import com.kronos.olympusfront.network.dto.RefreshTokenRequest;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Intercepte les réponses 401 : tente un renouvellement de l'access token via le refresh token,
 * puis rejoue la requête. En cas d'échec du refresh, déconnecte l'utilisateur.
 */
public class TokenRefreshAuthenticator implements Authenticator {

    private static final String TAG = "TokenRefreshAuth";

    private final Context appContext;
    private final TokenManager tokenManager;
    private final ApiService bareApiService;

    public TokenRefreshAuthenticator(Context context) {
        this.appContext = context.getApplicationContext();
        this.tokenManager = new TokenManager(appContext);
        // Client SANS authenticator ni interceptor d'auth pour éviter toute récursion
        OkHttpClient bareClient = new OkHttpClient.Builder().build();
        this.bareApiService = new Retrofit.Builder()
                .baseUrl(RetrofitClient.BASE_URL)
                .client(bareClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService.class);
    }

    @Override
    public synchronized Request authenticate(Route route, Response response) throws IOException {
        // Évite les boucles infinies : on ne tente le refresh qu'une seule fois
        if (responseCount(response) >= 2) {
            return null;
        }

        // Un 401 sur les endpoints d'authentification (login, register, refresh) n'a rien
        // à voir avec un access token expiré : on ne tente pas de refresh ni de logout.
        if (response.request().url().encodedPath().contains("/api/v1/auth/")) {
            return null;
        }

        String refreshToken = tokenManager.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            forceLogout();
            return null;
        }

        String currentAccess = tokenManager.getToken();
        String requestAuth = response.request().header("Authorization");

        // Un autre thread a peut-être déjà rafraîchi le token pendant l'attente du verrou
        if (currentAccess != null && requestAuth != null
                && !requestAuth.equals("Bearer " + currentAccess)) {
            return response.request().newBuilder()
                    .header("Authorization", "Bearer " + currentAccess)
                    .build();
        }

        // Appel synchrone du endpoint de rafraîchissement
        retrofit2.Response<AuthResponse> refreshResponse =
                bareApiService.refreshToken(new RefreshTokenRequest(refreshToken)).execute();

        if (refreshResponse.isSuccessful() && refreshResponse.body() != null
                && refreshResponse.body().getToken() != null) {
            AuthResponse body = refreshResponse.body();
            tokenManager.saveTokens(body.getToken(), body.getRefreshToken());
            Log.d(TAG, "Access token rafraîchi avec succès.");
            return response.request().newBuilder()
                    .header("Authorization", "Bearer " + body.getToken())
                    .build();
        }

        Log.w(TAG, "Échec du rafraîchissement (" + refreshResponse.code() + "), déconnexion.");
        forceLogout();
        return null;
    }

    private void forceLogout() {
        tokenManager.clearToken();
        Intent intent = new Intent(appContext, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        appContext.startActivity(intent);
    }

    private int responseCount(Response response) {
        int count = 1;
        Response prior = response.priorResponse();
        while (prior != null) {
            count++;
            prior = prior.priorResponse();
        }
        return count;
    }
}
