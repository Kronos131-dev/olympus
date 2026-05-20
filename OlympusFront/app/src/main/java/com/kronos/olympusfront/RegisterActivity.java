package com.kronos.olympusfront;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
            binding.idealCard3.setStrokeWidth(0);
            binding.idealTitle1.setTextColor(getResources().getColor(R.color.marble_white, null));
            binding.idealTitle2.setTextColor(getResources().getColor(R.color.outline_variant, null));
            binding.idealTitle3.setTextColor(getResources().getColor(R.color.outline_variant, null));
        });
        
        binding.idealCard2.setOnClickListener(v -> {
            selectedGoal = "LOSE_WEIGHT";
            binding.idealCard2.setStrokeWidth(2);
            binding.idealCard1.setStrokeWidth(0);
            binding.idealCard3.setStrokeWidth(0);
            binding.idealTitle2.setTextColor(getResources().getColor(R.color.marble_white, null));
            binding.idealTitle1.setTextColor(getResources().getColor(R.color.outline_variant, null));
            binding.idealTitle3.setTextColor(getResources().getColor(R.color.outline_variant, null));
        });

        binding.idealCard3.setOnClickListener(v -> {
            selectedGoal = "MAINTAIN";
            binding.idealCard3.setStrokeWidth(2);
            binding.idealCard1.setStrokeWidth(0);
            binding.idealCard2.setStrokeWidth(0);
            binding.idealTitle3.setTextColor(getResources().getColor(R.color.marble_white, null));
            binding.idealTitle1.setTextColor(getResources().getColor(R.color.outline_variant, null));
            binding.idealTitle2.setTextColor(getResources().getColor(R.color.outline_variant, null));
        });

        // Configurer le Spinner d'activité physique
        String[] activityLevels = {"Sédentaire (SEDENTARY)", "Léger (LIGHT)", "Modéré (MODERATE)", "Intense (INTENSE)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, activityLevels);
        binding.spinnerActivityLevel.setAdapter(adapter);
        binding.spinnerActivityLevel.setSelection(2); // Select MODERATE by default
        
        binding.spinnerActivityLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: selectedActivityLevel = "SEDENTARY"; break;
                    case 1: selectedActivityLevel = "LIGHT"; break;
                    case 2: selectedActivityLevel = "MODERATE"; break;
                    case 3: selectedActivityLevel = "INTENSE"; break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Default is already set
            }
        });

        binding.btnRegister.setOnClickListener(v -> performRegistration());
    }

    private void performRegistration() {
        String pseudo = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String heightStr = binding.etHeight.getText().toString();
        String weightStr = binding.etWeight.getText().toString();
        
        String day = binding.etBirthDay.getText().toString().trim();
        String month = binding.etBirthMonth.getText().toString().trim();
        String year = binding.etBirthYear.getText().toString().trim();

        if (pseudo.isEmpty() || password.isEmpty() || heightStr.isEmpty() || weightStr.isEmpty() || 
            day.isEmpty() || month.isEmpty() || year.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (password.length() < 6) {
             Toast.makeText(this, "Le mot de passe doit faire au moins 6 caractères", Toast.LENGTH_SHORT).show();
             return;
        }
        
        // Formatage de la date YYYY-MM-DD
        if(day.length() == 1) day = "0" + day;
        if(month.length() == 1) month = "0" + month;
        String birthDate = year + "-" + month + "-" + day;

        double height = Double.parseDouble(heightStr);
        double weight = Double.parseDouble(weightStr);

        binding.btnRegister.setEnabled(false);
        binding.btnRegister.setText("INSCRIPTION EN COURS...");

        RegisterRequest request = new RegisterRequest(
                pseudo, password, selectedGender, height, weight, birthDate, selectedActivityLevel, selectedGoal
        );
        
        Log.d(TAG, "Sending registration request for pseudo: " + pseudo);

        RetrofitClient.getApiService().register(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                binding.btnRegister.setEnabled(true);
                binding.btnRegister.setText("S'INSCRIRE");
                
                if (response.isSuccessful() && response.body() != null) {
                    tokenManager.saveTokens(response.body().getToken(), response.body().getRefreshToken());
                    Log.d(TAG, "Registration successful, tokens saved.");
                    Toast.makeText(RegisterActivity.this, "Inscription réussie !", Toast.LENGTH_SHORT).show();

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
                binding.btnRegister.setText("S'INSCRIRE");
                Log.e(TAG, "Registration network error", t);
                Toast.makeText(RegisterActivity.this, "Échec du réseau : " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}