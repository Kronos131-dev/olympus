package com.kronos.olympusfront;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.kronos.olympusfront.databinding.FragmentMealsBinding;
import com.kronos.olympusfront.network.RetrofitClient;
import com.kronos.olympusfront.network.dto.DailyLogResponse;
import com.kronos.olympusfront.network.dto.LogEntryRequest;
import com.kronos.olympusfront.network.dto.MealPresetResponse;

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
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Fetch presets every time we come back to this tab
        fetchMealPresets();
    }

    private void fetchMealPresets() {
        RetrofitClient.getApiService().getUserMealPresets().enqueue(new Callback<List<MealPresetResponse>>() {
            @Override
            public void onResponse(Call<List<MealPresetResponse>> call, Response<List<MealPresetResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setMeals(response.body());
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}