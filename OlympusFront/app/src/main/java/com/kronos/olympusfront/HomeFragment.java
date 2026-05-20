package com.kronos.olympusfront;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.kronos.olympusfront.databinding.FragmentHomeBinding;
import com.kronos.olympusfront.network.RetrofitClient;
import com.kronos.olympusfront.network.dto.DailyLogResponse;
import com.kronos.olympusfront.network.dto.LogEntryResponse;
import com.kronos.olympusfront.network.dto.UpdateActivityRequest;
import com.kronos.olympusfront.network.dto.UserResponse;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private LogsAdapter logsAdapter;
    private UserResponse currentUser;
    private DailyLogResponse currentDailyLog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        logsAdapter = new LogsAdapter(this::showDeleteConfirmationDialog);
        binding.recyclerRecentLogs.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerRecentLogs.setAdapter(logsAdapter);

        // Configuration des boutons
        binding.addMealButton.setOnClickListener(v -> {
            // Le bouton "AJOUTER REPAS" redirige vers l'onglet "Meals" (qui contient les presets) via l'activité principale
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.findViewById(R.id.nav_meals).performClick();
            }
        });

        // Un seul bouton pour Ajouter un Aliment (Manuel / Scan)
        binding.addFoodButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddFoodActivity.class);
            startActivity(intent);
        });

        // Édition de l'activité du jour (Pas et Entraînement)
        binding.btnEditActivity.setOnClickListener(v -> showEditActivityDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        // Toujours recharger les données en revenant sur l'écran
        fetchProfileAndLogs();
    }

    private void fetchProfileAndLogs() {
        // Fetch user profile first to get the target macros
        RetrofitClient.getApiService().getProfile().enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();
                    updateTargetUI();
                    fetchDailyLog(); // Fetch logs after getting profile
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Log.e(TAG, "Failed to fetch profile", t);
                fetchDailyLog(); // Attempt to fetch logs anyway
            }
        });
    }

    private void updateTargetUI() {
        if (!isAdded() || binding == null || currentUser == null) return;
        
        if (currentUser.getTargetKcal() != null) {
            int targetKcal = currentUser.getTargetKcal().intValue();
            binding.caloriesTargetText.setText("/ " + targetKcal + " KCAL");
            binding.caloriesProgress.setMax(targetKcal);
        }
        
        if (currentUser.getTargetProteins() != null) {
            int targetProt = currentUser.getTargetProteins().intValue();
            binding.proteinTargetText.setText("/ " + targetProt + "g");
            binding.proteinProgress.setMax(targetProt);
        }
        
        if (currentUser.getTargetCarbs() != null) {
            int targetCarbs = currentUser.getTargetCarbs().intValue();
            binding.carbsTargetText.setText("/ " + targetCarbs + "g");
            binding.carbsProgress.setMax(targetCarbs);
        }
        
        if (currentUser.getTargetFats() != null) {
            int targetFats = currentUser.getTargetFats().intValue();
            binding.fatsTargetText.setText("/ " + targetFats + "g");
            binding.fatsProgress.setMax(targetFats);
        }
    }

    private void fetchDailyLog() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        RetrofitClient.getApiService().getDailyLogByDate(today).enqueue(new Callback<DailyLogResponse>() {
            @Override
            public void onResponse(Call<DailyLogResponse> call, Response<DailyLogResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentDailyLog = response.body();
                    updateUI(currentDailyLog);
                } else {
                    Log.e(TAG, "Failed to fetch daily log: " + response.code());
                    if (isAdded() && getContext() != null) {
                        resetUI();
                    }
                }
            }

            @Override
            public void onFailure(Call<DailyLogResponse> call, Throwable t) {
                Log.e(TAG, "Network error", t);
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Erreur réseau", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showEditActivityDialog() {
        if (getContext() == null || currentDailyLog == null || currentUser == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.OlympusAlertDialogTheme);
        builder.setTitle("Activité de la journée");

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_activity, null);
        builder.setView(dialogView);

        EditText etSteps = dialogView.findViewById(R.id.et_steps);
        EditText etWorkout = dialogView.findViewById(R.id.et_workout);
        EditText etManualBurned = dialogView.findViewById(R.id.et_manual_kcal_burned);
        TextView tvStepInfo = dialogView.findViewById(R.id.tv_step_goal_info);

        int minSteps = 8000;
        if (currentUser.getActivityLevel() != null) {
            switch (currentUser.getActivityLevel()) {
                case "SEDENTARY": minSteps = 3000; break;
                case "LIGHT": minSteps = 5000; break;
                case "MODERATE": minSteps = 8000; break;
                case "INTENSE": minSteps = 10000; break;
            }
        }
        tvStepInfo.setText("Objectif de base (" + currentUser.getActivityLevel() + ") : " + minSteps + " pas/jour. Les pas en dessous pénalisent, au-dessus donnent un bonus.");

        etSteps.setText(String.valueOf(currentDailyLog.getStepCount() != null ? currentDailyLog.getStepCount() : 0));
        etWorkout.setText(String.valueOf(currentDailyLog.getWorkoutDurationMinutes() != null ? currentDailyLog.getWorkoutDurationMinutes() : 0));
        etManualBurned.setText(String.valueOf(currentDailyLog.getManualKcalBurned() != null ? currentDailyLog.getManualKcalBurned() : 0));

        builder.setPositiveButton("Enregistrer", (dialog, which) -> {
            String stepsStr = etSteps.getText().toString();
            String workoutStr = etWorkout.getText().toString();
            String manualStr = etManualBurned.getText().toString();

            int steps = stepsStr.isEmpty() ? 0 : Integer.parseInt(stepsStr);
            int workout = workoutStr.isEmpty() ? 0 : Integer.parseInt(workoutStr);
            int manualBurned = manualStr.isEmpty() ? 0 : Integer.parseInt(manualStr);

            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            UpdateActivityRequest request = new UpdateActivityRequest(today, steps, workout, manualBurned);

            RetrofitClient.getApiService().updateActivity(request).enqueue(new Callback<DailyLogResponse>() {
                @Override
                public void onResponse(Call<DailyLogResponse> call, Response<DailyLogResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        currentDailyLog = response.body();
                        updateUI(currentDailyLog);
                        Toast.makeText(getContext(), "Activité mise à jour", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Erreur lors de la sauvegarde", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<DailyLogResponse> call, Throwable t) {
                    Toast.makeText(getContext(), "Erreur réseau", Toast.LENGTH_SHORT).show();
                }
            });
        });
        builder.setNegativeButton("Annuler", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showDeleteConfirmationDialog(LogEntryResponse logEntry) {
        if (getContext() == null) return;

        String name = "";
        if (logEntry.getMealPreset() != null) {
            name = logEntry.getMealPreset().getName();
        } else if (logEntry.getFoodItem() != null) {
            name = logEntry.getFoodItem().getName();
        }

        new AlertDialog.Builder(getContext(), R.style.OlympusAlertDialogTheme)
            .setTitle("Supprimer la consommation ?")
            .setMessage("Voulez-vous vraiment retirer '" + name + "' de votre journée ?\nLes macros et calories seront reculées.")
            .setPositiveButton("Supprimer", (dialog, which) -> deleteLogEntry(logEntry.getId()))
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void deleteLogEntry(Long entryId) {
        RetrofitClient.getApiService().removeLogEntry(entryId).enqueue(new Callback<DailyLogResponse>() {
            @Override
            public void onResponse(Call<DailyLogResponse> call, Response<DailyLogResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(getContext(), "Consommation retirée", Toast.LENGTH_SHORT).show();
                    currentDailyLog = response.body();
                    updateUI(currentDailyLog);
                } else {
                    Toast.makeText(getContext(), "Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DailyLogResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetUI() {
        if (!isAdded() || binding == null) return;
        
        binding.caloriesText.setText("0");
        binding.caloriesProgress.setProgress(0);

        binding.proteinText.setText("0g");
        binding.proteinProgress.setProgress(0);

        binding.carbsText.setText("0g");
        binding.carbsProgress.setProgress(0);

        binding.fatsText.setText("0g");
        binding.fatsProgress.setProgress(0);

        binding.tvSteps.setText("0 PAS");
        binding.tvWorkout.setText("0 MIN");
        binding.tvBurnedKcal.setVisibility(View.VISIBLE); // Now always visible
        binding.tvBurnedKcal.setText("🔥 0 KCAL BRÛLÉES");
        
        logsAdapter.setLogs(java.util.Collections.emptyList());
    }

    private void updateUI(DailyLogResponse log) {
        if (!isAdded() || binding == null) return;

        double kcal = log.getTotalKcal() != null ? log.getTotalKcal() : 0.0;
        double proteins = log.getTotalProteins() != null ? log.getTotalProteins() : 0.0;
        double carbs = log.getTotalCarbs() != null ? log.getTotalCarbs() : 0.0;
        double fats = log.getTotalFats() != null ? log.getTotalFats() : 0.0;

        // Mise à jour de l'activité
        int steps = log.getStepCount() != null ? log.getStepCount() : 0;
        int workout = log.getWorkoutDurationMinutes() != null ? log.getWorkoutDurationMinutes() : 0;
        double burned = log.getExtraKcalBurned() != null ? log.getExtraKcalBurned() : 0.0;

        binding.tvSteps.setText(steps + " PAS");
        binding.tvWorkout.setText(workout + " MIN");
        
        // Always visible as requested
        binding.tvBurnedKcal.setVisibility(View.VISIBLE);
        
        if (burned != 0 && currentUser != null && currentUser.getTargetKcal() != null) {
            String sign = burned > 0 ? "+" : "";
            binding.tvBurnedKcal.setText(String.format(Locale.US, "🔥 %s%.0f KCAL BRÛLÉES", sign, burned));
            
            // Adjust max target dynamically 
            int targetKcal = currentUser.getTargetKcal().intValue();
            int adjustedTarget = targetKcal + (int) burned;
            // Ne jamais descendre en dessous du BMR absolu ou de 0, par sécurité
            if (adjustedTarget < 1000) adjustedTarget = 1000;

            binding.caloriesTargetText.setText("/ " + adjustedTarget + " KCAL");
            binding.caloriesProgress.setMax(adjustedTarget);

        } else {
            binding.tvBurnedKcal.setText("🔥 0 KCAL BRÛLÉES");
            updateTargetUI(); // Reset to normal targets
        }

        // Mise à jour de l'anneau central
        binding.caloriesText.setText(String.valueOf((int) kcal));
        binding.caloriesProgress.setProgress((int) kcal);

        // Mise à jour des barres de macros
        binding.proteinText.setText(String.valueOf((int) proteins) + "g");
        binding.proteinProgress.setProgress((int) proteins);

        binding.carbsText.setText(String.valueOf((int) carbs) + "g");
        binding.carbsProgress.setProgress((int) carbs);

        binding.fatsText.setText(String.valueOf((int) fats) + "g");
        binding.fatsProgress.setProgress((int) fats);

        // Mettre à jour la liste des consommations
        if (log.getEntries() != null && !log.getEntries().isEmpty()) {
            logsAdapter.setLogs(log.getEntries());
        } else {
            logsAdapter.setLogs(java.util.Collections.emptyList());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}