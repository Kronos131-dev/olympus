package com.kronos.olympusfront;

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

import com.kronos.olympusfront.databinding.FragmentHomeBinding;
import com.kronos.olympusfront.network.RetrofitClient;
import com.kronos.olympusfront.network.dto.DailyLogResponse;
import com.kronos.olympusfront.network.dto.LogEntryResponse;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private LogsAdapter logsAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        logsAdapter = new LogsAdapter();
        binding.recyclerRecentLogs.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerRecentLogs.setAdapter(logsAdapter);

        fetchDailyLog();
    }

    private void fetchDailyLog() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        RetrofitClient.getApiService().getDailyLogByDate(today).enqueue(new Callback<DailyLogResponse>() {
            @Override
            public void onResponse(Call<DailyLogResponse> call, Response<DailyLogResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateUI(response.body());
                } else {
                    Log.e(TAG, "Failed to fetch daily log: " + response.code());
                    if (isAdded() && getContext() != null) {
                        // Pas de Toast ici, car si c'est un nouveau jour, un 404 est normal (pas encore de données).
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

    private void updateUI(DailyLogResponse log) {
        if (!isAdded() || binding == null) return;

        double kcal = log.getTotalKcal() != null ? log.getTotalKcal() : 0.0;
        double proteins = log.getTotalProteins() != null ? log.getTotalProteins() : 0.0;
        double carbs = log.getTotalCarbs() != null ? log.getTotalCarbs() : 0.0;
        double fats = log.getTotalFats() != null ? log.getTotalFats() : 0.0;

        // Mise à jour de l'anneau central
        binding.caloriesText.setText(String.valueOf((int) kcal));
        binding.caloriesProgress.setProgress((int) kcal);

        // Mise à jour des barres de macros
        binding.proteinText.setText((int) proteins + "g");
        binding.proteinProgress.setProgress((int) proteins);

        binding.carbsText.setText((int) carbs + "g");
        binding.carbsProgress.setProgress((int) carbs);

        binding.fatsText.setText((int) fats + "g");
        binding.fatsProgress.setProgress((int) fats);

        // Mettre à jour la liste des consommations
        if (log.getEntries() != null && !log.getEntries().isEmpty()) {
            logsAdapter.setLogs(log.getEntries());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}