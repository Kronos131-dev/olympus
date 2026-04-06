package com.kronos.olympusfront;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.kronos.olympusfront.databinding.ActivityRegisterBinding;
import com.kronos.olympusfront.network.RetrofitClient;
import com.kronos.olympusfront.network.TokenManager;
import com.kronos.olympusfront.network.dto.AuthResponse;
import com.kronos.olympusfront.network.dto.RegisterRequest;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private ActivityRegisterBinding binding;
    private String selectedGender = "MALE";
    private String selectedGoal = "GAIN_MUSCLE";
    private String selectedActivityLevel = "MODERATE"; // Valeur par défaut
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tokenManager = new TokenManager(this);

        binding.btnClose.setOnClickListener(v -> finish());

        // Gérer la sélection du genre
        binding.btnMale.setOnClickListener(v -> {
            selectedGender = "MALE";
            binding.btnMale.setTextColor(getResources().getColor(R.color.secondary, null));
            binding.btnFemale.setTextColor(getResources().getColor(R.color.outline_variant, null));
        });

        binding.btnFemale.setOnClickListener(v -> {
            selectedGender = "FEMALE";
            binding.btnFemale.setTextColor(getResources().getColor(R.color.secondary, null));
            binding.btnMale.setTextColor(getResources().getColor(R.color.outline_variant, null));
        });

        // Gérer la sélection de l'objectif
        binding.idealCard1.setOnClickListener(v -> {
            selectedGoal = "GAIN_MUSCLE";
            binding.idealCard1.setStrokeWidth(2);
            binding.idealCard2.setStrokeWidth(0);
            binding.idealTitle1.setTextColor(getResources().getColor(R.color.marble_white, null));
            binding.idealTitle2.setTextColor(getResources().getColor(R.color.outline_variant, null));
        });
        
        binding.idealCard2.setOnClickListener(v -> {
            selectedGoal = "LOSE_WEIGHT";
            binding.idealCard2.setStrokeWidth(2);
            binding.idealCard1.setStrokeWidth(0);
            binding.idealTitle2.setTextColor(getResources().getColor(R.color.marble_white, null));
            binding.idealTitle1.setTextColor(getResources().getColor(R.color.outline_variant, null));
        });

        binding.btnRegister.setOnClickListener(v -> performRegistration());
    }

    private void performRegistration() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String heightStr = binding.etHeight.getText().toString();
        String weightStr = binding.etWeight.getText().toString();

        if (email.isEmpty() || password.isEmpty() || heightStr.isEmpty() || weightStr.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (password.length() < 6) {
             Toast.makeText(this, "Le mot de passe doit faire au moins 6 caractères", Toast.LENGTH_SHORT).show();
             return;
        }

        double height = Double.parseDouble(heightStr);
        double weight = Double.parseDouble(weightStr);

        binding.btnRegister.setEnabled(false);
        binding.btnRegister.setText("SCELLAGE EN COURS...");

        RegisterRequest request = new RegisterRequest(
                email, password, selectedGender, height, weight, selectedActivityLevel, selectedGoal
        );
        
        Log.d(TAG, "Sending registration request for email: " + email);

        RetrofitClient.getApiService().register(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                binding.btnRegister.setEnabled(true);
                binding.btnRegister.setText("SCELLER LE PACTE");
                
                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().getToken();
                    tokenManager.saveToken(token);
                    Log.d(TAG, "Registration successful. Token: " + token);
                    Toast.makeText(RegisterActivity.this, "Pacte scellé avec succès !", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    String errorMsg = "Erreur inconnue";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.e(TAG, "Registration failed: Code " + response.code() + " - Body: " + errorMsg);
                    
                    if(response.code() == 400 || response.code() == 409 || response.code() == 403) {
                        Toast.makeText(RegisterActivity.this, "Erreur " + response.code() + ": " + errorMsg, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Erreur serveur: " + response.code(), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                binding.btnRegister.setEnabled(true);
                binding.btnRegister.setText("SCELLER LE PACTE");
                Log.e(TAG, "Registration network error", t);
                Toast.makeText(RegisterActivity.this, "Échec du réseau : " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}