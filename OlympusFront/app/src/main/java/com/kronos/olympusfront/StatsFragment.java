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

import com.kronos.olympusfront.databinding.FragmentStatsBinding;
import com.kronos.olympusfront.network.RetrofitClient;
import com.kronos.olympusfront.network.dto.AnalyticsResponse;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatsFragment extends Fragment {

    private static final String TAG = "StatsFragment";
    private FragmentStatsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStatsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fetchAnalytics();
    }

    private void fetchAnalytics() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        Calendar cal = Calendar.getInstance();
        String endDate = sdf.format(cal.getTime());
        
        cal.add(Calendar.DAY_OF_YEAR, -7);
        String startDate = sdf.format(cal.getTime());

        RetrofitClient.getApiService().getAnalyticsForPeriod(startDate, endDate).enqueue(new Callback<AnalyticsResponse>() {
            @Override
            public void onResponse(Call<AnalyticsResponse> call, Response<AnalyticsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateUI(response.body());
                } else {
                    Log.e(TAG, "Erreur récupération stats: " + response.code());
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Impossible de récupérer les stats", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<AnalyticsResponse> call, Throwable t) {
                Log.e(TAG, "Erreur réseau (stats)", t);
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Erreur réseau", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateUI(AnalyticsResponse analytics) {
        if (!isAdded() || binding == null) return;
        
        // Exemple simple : on pourrait mettre à jour des TextViews spécifiques
        // Comme les IDs manquent dans le Layout (sauf des IDs génériques), on garde ça simple.
        // Dans un cas réel, on utiliserait un LineChart (ex: MPAndroidChart) pour afficher `analytics.getDailyData()`
        Log.d(TAG, "Moyenne Kcal: " + analytics.getAverageKcal());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}