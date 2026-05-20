package com.kronos.olympusfront;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.kronos.olympusfront.databinding.FragmentProfileBinding;
import com.kronos.olympusfront.network.RetrofitClient;
import com.kronos.olympusfront.network.TokenManager;
import com.kronos.olympusfront.network.dto.LogoutRequest;
import com.kronos.olympusfront.network.dto.UserResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private FragmentProfileBinding binding;
    private UserResponse currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        fetchProfile();

        binding.editBiometricsButton.setOnClickListener(v -> {
            showEditTargetsDialog();
        });

        binding.btnEditTargets.setOnClickListener(v -> {
            showEditTargetsDialog();
        });

        binding.logoutButton.setOnClickListener(v -> {
            if (getContext() != null) {
                TokenManager tokenManager = new TokenManager(getContext());

                // Révocation côté serveur du refresh token (fire-and-forget)
                String refreshToken = tokenManager.getRefreshToken();
                if (refreshToken != null && !refreshToken.isEmpty()) {
                    RetrofitClient.getApiService().logout(new LogoutRequest(refreshToken))
                            .enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(Call<Void> call, Response<Void> response) { }
                                @Override
                                public void onFailure(Call<Void> call, Throwable t) { }
                            });
                }

                tokenManager.clearToken();

                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

        // Bouton d'édition rapide du poids
        binding.btnEditWeight.setOnClickListener(v -> showEditWeightDialog());
        
        // Bouton d'édition rapide de la taille
        binding.btnEditHeight.setOnClickListener(v -> showEditHeightDialog());
    }

    private void fetchProfile() {
        RetrofitClient.getApiService().getProfile().enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();
                    updateUI(currentUser);
                } else {
                    Log.e(TAG, "Erreur récupération profil: " + response.code());
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Impossible de récupérer le profil", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Log.e(TAG, "Erreur réseau (profil)", t);
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Erreur réseau", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateUI(UserResponse user) {
        if (!isAdded() || binding == null) return;

        // Nom
        String name = user.getEmail().split("@")[0];
        name = name.substring(0, 1).toUpperCase() + name.substring(1); // Capitalize first letter
        binding.profileName.setText(name);

        // Vocation / Activité
        String goalText = user.getGoal() != null ? user.getGoal().replace("_", " ") : "MAINTAIN";
        binding.profileObjective.setText(goalText);
        binding.profileActivity.setText(user.getActivityLevel() != null ? user.getActivityLevel() : "MODERATE");

        // Mensurations
        if (user.getHeightCm() != null) {
            binding.profileHeight.setText(String.valueOf(user.getHeightCm().intValue()));
        }
        if (user.getCurrentWeightKg() != null) {
            binding.profileWeight.setText(String.valueOf(user.getCurrentWeightKg()));
        }
        
        String natureText = user.getGender() != null && user.getGender().equals("MALE") ? "MASCULIN" : "FÉMININ";
        binding.profileNature.setText(natureText);

        // Cibles nutritionnelles calculées ou manuelles
        if (user.getTargetKcal() != null) {
            binding.profileTargetKcal.setText(String.valueOf(user.getTargetKcal().intValue()));
        }
        if (user.getTargetProteins() != null) {
            binding.profileTargetProt.setText(String.valueOf(user.getTargetProteins().intValue()) + "g");
        }
        if (user.getTargetCarbs() != null) {
            binding.profileTargetCarbs.setText(String.valueOf(user.getTargetCarbs().intValue()) + "g");
        }
        if (user.getTargetFats() != null) {
            binding.profileTargetFats.setText(String.valueOf(user.getTargetFats().intValue()) + "g");
        }
    }

    private void showEditWeightDialog() {
        if (getContext() == null || currentUser == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.OlympusAlertDialogTheme);
        builder.setTitle("Nouveau Poids (KG)");

        final EditText input = new EditText(getContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.valueOf(currentUser.getCurrentWeightKg()));
        builder.setView(input);

        builder.setPositiveButton("Enregistrer", (dialog, which) -> {
            String newWeightStr = input.getText().toString();
            if (!newWeightStr.isEmpty()) {
                double newWeight = Double.parseDouble(newWeightStr);
                // On met à jour l'objet local
                currentUser.setCurrentWeightKg(newWeight);
                
                // On appelle l'API pour sauvegarder la nouvelle valeur
                RetrofitClient.getApiService().updateProfile(currentUser).enqueue(new Callback<UserResponse>() {
                    @Override
                    public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            currentUser = response.body(); // Update current user with new targets
                            updateUI(currentUser);
                            Toast.makeText(getContext(), "Poids et cibles mis à jour", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Erreur lors de la sauvegarde", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<UserResponse> call, Throwable t) {
                        Toast.makeText(getContext(), "Erreur réseau", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        builder.setNegativeButton("Annuler", (dialog, which) -> dialog.cancel());

        builder.show();
    }
    
    private void showEditHeightDialog() {
        if (getContext() == null || currentUser == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.OlympusAlertDialogTheme);
        builder.setTitle("Nouvelle Taille (CM)");

        final EditText input = new EditText(getContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(currentUser.getHeightCm().intValue()));
        builder.setView(input);

        builder.setPositiveButton("Enregistrer", (dialog, which) -> {
            String newHeightStr = input.getText().toString();
            if (!newHeightStr.isEmpty()) {
                double newHeight = Double.parseDouble(newHeightStr);
                // On met à jour l'objet local
                currentUser.setHeightCm(newHeight);
                
                // On appelle l'API pour sauvegarder la nouvelle valeur
                RetrofitClient.getApiService().updateProfile(currentUser).enqueue(new Callback<UserResponse>() {
                    @Override
                    public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            currentUser = response.body(); // Update current user with new targets
                            updateUI(currentUser);
                            Toast.makeText(getContext(), "Taille et cibles mises à jour", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Erreur lors de la sauvegarde", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<UserResponse> call, Throwable t) {
                        Toast.makeText(getContext(), "Erreur réseau", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        builder.setNegativeButton("Annuler", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showEditTargetsDialog() {
        if (getContext() == null || currentUser == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.OlympusAlertDialogTheme);
        builder.setTitle("Configuration des Cibles");

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_targets, null);
        builder.setView(dialogView);

        MaterialSwitch switchAuto = dialogView.findViewById(R.id.switch_auto_calc);
        View containerAuto = dialogView.findViewById(R.id.container_auto_settings);
        View containerManual = dialogView.findViewById(R.id.container_manual_settings);

        Spinner spinnerGoal = dialogView.findViewById(R.id.spinner_goal);
        Spinner spinnerActivity = dialogView.findViewById(R.id.spinner_activity);
        
        EditText etManualKcal = dialogView.findViewById(R.id.et_manual_kcal);
        EditText etManualProt = dialogView.findViewById(R.id.et_manual_prot);
        EditText etManualCarbs = dialogView.findViewById(R.id.et_manual_carbs);
        EditText etManualFats = dialogView.findViewById(R.id.et_manual_fats);

        String[] goals = {"GAIN_MUSCLE", "LOSE_WEIGHT", "MAINTAIN"};
        ArrayAdapter<String> goalAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, goals);
        spinnerGoal.setAdapter(goalAdapter);

        String[] activities = {"SEDENTARY", "LIGHT", "MODERATE", "INTENSE"};
        ArrayAdapter<String> activityAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, activities);
        spinnerActivity.setAdapter(activityAdapter);

        // Pre-fill
        switchAuto.setChecked(currentUser.getAutoCalculateTargets() == null || currentUser.getAutoCalculateTargets());
        
        if (currentUser.getGoal() != null) {
            for (int i = 0; i < goals.length; i++) {
                if (goals[i].equals(currentUser.getGoal())) { spinnerGoal.setSelection(i); break; }
            }
        }
        if (currentUser.getActivityLevel() != null) {
            for (int i = 0; i < activities.length; i++) {
                if (activities[i].equals(currentUser.getActivityLevel())) { spinnerActivity.setSelection(i); break; }
            }
        }

        // Fill manual target fields with current targets so the user has a starting point
        if (currentUser.getTargetKcal() != null) etManualKcal.setText(String.valueOf(currentUser.getTargetKcal().intValue()));
        if (currentUser.getTargetProteins() != null) etManualProt.setText(String.valueOf(currentUser.getTargetProteins().intValue()));
        if (currentUser.getTargetCarbs() != null) etManualCarbs.setText(String.valueOf(currentUser.getTargetCarbs().intValue()));
        if (currentUser.getTargetFats() != null) etManualFats.setText(String.valueOf(currentUser.getTargetFats().intValue()));

        // Toggle visibility
        Runnable updateVisibility = () -> {
            if (switchAuto.isChecked()) {
                containerAuto.setVisibility(View.VISIBLE);
                containerManual.setVisibility(View.GONE);
            } else {
                containerAuto.setVisibility(View.GONE);
                containerManual.setVisibility(View.VISIBLE);
            }
        };
        updateVisibility.run();
        switchAuto.setOnCheckedChangeListener((buttonView, isChecked) -> updateVisibility.run());

        builder.setPositiveButton("Enregistrer", (dialog, which) -> {
            com.kronos.olympusfront.network.dto.UserResponse updatedUser = new com.kronos.olympusfront.network.dto.UserResponse();
            updatedUser.setId(currentUser.getId());
            updatedUser.setEmail(currentUser.getEmail());
            updatedUser.setHeightCm(currentUser.getHeightCm());
            updatedUser.setCurrentWeightKg(currentUser.getCurrentWeightKg());
            updatedUser.setGender(currentUser.getGender() != null ? currentUser.getGender() : "MALE");

            updatedUser.setAutoCalculateTargets(switchAuto.isChecked());

            if (switchAuto.isChecked()) {
                updatedUser.setGoal((String) spinnerGoal.getSelectedItem());
                updatedUser.setActivityLevel((String) spinnerActivity.getSelectedItem());
            } else {
                updatedUser.setGoal(currentUser.getGoal()); // Keep old values
                updatedUser.setActivityLevel(currentUser.getActivityLevel());
                
                try {
                    updatedUser.setManualTargetKcal(Double.parseDouble(etManualKcal.getText().toString()));
                    updatedUser.setManualTargetProteins(Double.parseDouble(etManualProt.getText().toString()));
                    updatedUser.setManualTargetCarbs(Double.parseDouble(etManualCarbs.getText().toString()));
                    updatedUser.setManualTargetFats(Double.parseDouble(etManualFats.getText().toString()));
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Veuillez entrer des nombres valides", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            RetrofitClient.getApiService().updateProfile(updatedUser).enqueue(new Callback<UserResponse>() {
                @Override
                public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        currentUser = response.body();
                        updateUI(currentUser);
                        Toast.makeText(getContext(), "Cibles mises à jour", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Erreur lors de la sauvegarde", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<UserResponse> call, Throwable t) {
                    Toast.makeText(getContext(), "Erreur réseau", Toast.LENGTH_SHORT).show();
                }
            });
        });
        builder.setNegativeButton("Annuler", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}