package com.kronos.olympusfront;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.gson.Gson;
import com.kronos.olympusfront.databinding.FragmentMealsBinding;
import com.kronos.olympusfront.network.RetrofitClient;
import com.kronos.olympusfront.network.dto.DailyLogResponse;
import com.kronos.olympusfront.network.dto.LogEntryRequest;
import com.kronos.olympusfront.network.dto.MealPresetResponse;
import com.kronos.olympusfront.network.dto.UserResponse;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MealsFragment extends Fragment implements MealsAdapter.OnMealClickListener {

    private static final String TAG = "MealsFragment";
    private FragmentMealsBinding binding;
    private MealsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMealsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Configuration du RecyclerView
        adapter = new MealsAdapter(this);
        binding.recyclerMeals.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerMeals.setAdapter(adapter);

        binding.newMealButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CreateMealActivity.class);
            startActivity(intent);
        });

        // Création d'un repas assistée par l'IA : ouvre directement le dialogue d'analyse IA.
        binding.aiMealButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CreateMealActivity.class);
            intent.putExtra("EXTRA_START_AI", true);
            startActivity(intent);
        });

        binding.searchMeals.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    adapter.filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Fetch user profile first to get targets for progress bars
        fetchProfileAndPresets();
    }

    private void fetchProfileAndPresets() {
        RetrofitClient.getApiService().getProfile().enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse user = response.body();
                    int kcal = user.getTargetKcal() != null ? user.getTargetKcal().intValue() : 2500;
                    int prot = user.getTargetProteins() != null ? user.getTargetProteins().intValue() : 180;
                    int carbs = user.getTargetCarbs() != null ? user.getTargetCarbs().intValue() : 350;
                    int fats = user.getTargetFats() != null ? user.getTargetFats().intValue() : 85;
                    
                    adapter.setTargets(kcal, prot, carbs, fats);
                }
                // Fetch presets whether profile succeeds or fails
                fetchMealPresets();
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                fetchMealPresets();
            }
        });
    }

    private void fetchMealPresets() {
        RetrofitClient.getApiService().getUserMealPresets().enqueue(new Callback<List<MealPresetResponse>>() {
            @Override
            public void onResponse(Call<List<MealPresetResponse>> call, Response<List<MealPresetResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setMeals(response.body());
                    if (binding != null) {
                        adapter.filter(binding.searchMeals.getText().toString());
                    }
                } else {
                    Log.e(TAG, "Erreur récupération repas: " + response.code());
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Erreur serveur: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<MealPresetResponse>> call, Throwable t) {
                Log.e(TAG, "Erreur réseau (repas)", t);
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Erreur réseau: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onConsumeClick(MealPresetResponse meal) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        LogEntryRequest request = new LogEntryRequest();
        request.setTargetDate(today);
        request.setMealPresetId(meal.getId());
        
        Toast.makeText(getContext(), "Ajout de " + meal.getName() + " en cours...", Toast.LENGTH_SHORT).show();

        RetrofitClient.getApiService().addLogEntry(request).enqueue(new Callback<DailyLogResponse>() {
            @Override
            public void onResponse(Call<DailyLogResponse> call, Response<DailyLogResponse> response) {
                if (response.isSuccessful()) {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), meal.getName() + " consommé avec succès !", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "Erreur consommation: " + response.code());
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Erreur lors de l'ajout: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<DailyLogResponse> call, Throwable t) {
                Log.e(TAG, "Erreur réseau (consommation)", t);
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Échec réseau", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onEditClick(MealPresetResponse meal) {
        if (getContext() != null) {
            Intent intent = new Intent(getActivity(), CreateMealActivity.class);
            // On passe l'objet meal entier en JSON via l'intent pour le récupérer dans l'activité d'édition
            intent.putExtra("EXTRA_MEAL_PRESET", new Gson().toJson(meal));
            startActivity(intent);
        }
    }

    @Override
    public void onDeleteClick(MealPresetResponse meal) {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext(), R.style.OlympusAlertDialogTheme)
            .setTitle("Supprimer ce repas ?")
            .setMessage("Voulez-vous vraiment supprimer le repas '" + meal.getName() + "' ?")
            .setPositiveButton("Supprimer", (dialog, which) -> {
                RetrofitClient.getApiService().deleteMealPreset(meal.getId()).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Repas supprimé", Toast.LENGTH_SHORT).show();
                            fetchProfileAndPresets(); // Rafraîchir la liste
                        } else {
                            Toast.makeText(getContext(), "Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(getContext(), "Erreur réseau", Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}