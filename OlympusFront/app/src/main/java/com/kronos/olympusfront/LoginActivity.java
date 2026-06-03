package com.kronos.olympusfront;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.kronos.olympusfront.databinding.ActivityLoginBinding;
import com.kronos.olympusfront.network.RetrofitClient;
import com.kronos.olympusfront.network.TokenManager;
import com.kronos.olympusfront.network.dto.AuthRequest;
import com.kronos.olympusfront.network.dto.AuthResponse;
import com.kronos.olympusfront.network.dto.UserResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private ActivityLoginBinding binding;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tokenManager = new TokenManager(this);

        binding.btnEntrerArene.setOnClickListener(v -> performLogin());
        binding.tvCreerCompte.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        // Un token stocké n'est PAS une preuve de connexion : il peut être expiré.
        // On valide la session auprès du serveur avant d'ouvrir l'application.
        if (tokenManager.getToken() != null) {
            validateSession();
        }
    }

    /** Vérifie auprès du serveur que la session stockée est encore valide. */
    private void validateSession() {
        binding.btnEntrerArene.setEnabled(false);
        RetrofitClient.getApiService().getProfile().enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserResponse> call,
                                   @NonNull Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    goToMain();
                } else {
                    // Session refusée par le serveur : on nettoie et on reste sur l'écran de connexion.
                    Log.w(TAG, "Session invalide (" + response.code() + ")");
                    tokenManager.clearToken();
                    binding.btnEntrerArene.setEnabled(true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                // Échec réseau : on ne supprime pas la session (utilisateur peut-être hors ligne).
                Log.e(TAG, "Validation de session impossible", t);
                binding.btnEntrerArene.setEnabled(true);
                Toast.makeText(LoginActivity.this,
                        "Serveur injoignable. Vérifiez votre connexion.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnEntrerArene.setEnabled(false);

        AuthRequest request = new AuthRequest(email, password);
        RetrofitClient.getApiService().login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call,
                                   @NonNull Response<AuthResponse> response) {
                binding.btnEntrerArene.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    tokenManager.saveTokens(response.body().getToken(),
                            response.body().getRefreshToken());
                    Log.d(TAG, "Login successful, tokens saved.");
                    Toast.makeText(LoginActivity.this, "Connexion réussie", Toast.LENGTH_SHORT).show();
                    goToMain();
                } else {
                    Log.e(TAG, "Login failed: " + response.code());
                    Toast.makeText(LoginActivity.this, "Identifiants incorrects", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                binding.btnEntrerArene.setEnabled(true);
                Log.e(TAG, "Login error", t);
                Toast.makeText(LoginActivity.this,
                        "Erreur de connexion : " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goToMain() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}
