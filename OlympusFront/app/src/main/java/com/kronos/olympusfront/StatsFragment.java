package com.kronos.olympusfront;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.kronos.olympusfront.databinding.FragmentStatsBinding;
import com.kronos.olympusfront.network.RetrofitClient;
import com.kronos.olympusfront.network.TokenManager;
import com.kronos.olympusfront.network.dto.AnalyticsResponse;
import com.kronos.olympusfront.network.dto.UserResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatsFragment extends Fragment {

    private static final String TAG = "StatsFragment";
    private FragmentStatsBinding binding;
    private int daysToFetch = 7; // Default 1 Week
    private UserResponse currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStatsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupCharts();

        binding.togglePeriod.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_week) daysToFetch = 7;
                else if (checkedId == R.id.btn_month) daysToFetch = 30;
                else if (checkedId == R.id.btn_three_months) daysToFetch = 90;
                else if (checkedId == R.id.btn_year) daysToFetch = 365;
                fetchData();
            }
        });

        fetchData();
    }
    
    private void fetchData() {
        // Fetch profile first to get the target kcal, then fetch analytics
        RetrofitClient.getApiService().getProfile().enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();
                }
                fetchAnalytics();
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                fetchAnalytics();
            }
        });
    }

    private void setupCharts() {
        // BarChart for Calories
        binding.barChartCalories.getDescription().setEnabled(false);
        binding.barChartCalories.getLegend().setEnabled(true);
        binding.barChartCalories.getLegend().setTextColor(Color.WHITE);
        binding.barChartCalories.getAxisRight().setEnabled(false);
        binding.barChartCalories.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        binding.barChartCalories.getXAxis().setTextColor(Color.WHITE);
        binding.barChartCalories.getAxisLeft().setTextColor(Color.WHITE);
        binding.barChartCalories.setDrawGridBackground(false);

        // PieChart for Macros
        binding.pieChartMacros.getDescription().setEnabled(false);
        binding.pieChartMacros.setDrawHoleEnabled(true);
        binding.pieChartMacros.setHoleColor(Color.TRANSPARENT);
        binding.pieChartMacros.setTransparentCircleColor(Color.TRANSPARENT);
        binding.pieChartMacros.setEntryLabelColor(Color.WHITE);
        binding.pieChartMacros.getLegend().setTextColor(Color.WHITE);

        // LineChart for Weight
        binding.lineChartWeight.getDescription().setEnabled(false);
        binding.lineChartWeight.getLegend().setEnabled(false);
        binding.lineChartWeight.getAxisRight().setEnabled(false);
        binding.lineChartWeight.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        binding.lineChartWeight.getXAxis().setTextColor(Color.WHITE);
        binding.lineChartWeight.getAxisLeft().setTextColor(Color.WHITE);
    }

    private void fetchAnalytics() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        Calendar cal = Calendar.getInstance();
        String endDate = sdf.format(cal.getTime());
        
        cal.add(Calendar.DAY_OF_YEAR, -daysToFetch + 1); // +1 because it's inclusive
        String startDate = sdf.format(cal.getTime());

        RetrofitClient.getApiService().getAnalyticsForPeriod(startDate, endDate).enqueue(new Callback<AnalyticsResponse>() {
            @Override
            public void onResponse(Call<AnalyticsResponse> call, Response<AnalyticsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateCharts(response.body());
                } else {
                    Log.e(TAG, "Erreur stats: " + response.code());
                    if (isAdded()) Toast.makeText(getContext(), "Pas assez de données pour cette période", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AnalyticsResponse> call, Throwable t) {
                Log.e(TAG, "Network Error", t);
                if (isAdded()) Toast.makeText(getContext(), "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCharts(AnalyticsResponse data) {
        if (!isAdded() || binding == null || data.getDailyData() == null) return;

        List<AnalyticsResponse.DailyMetricPoint> dailyData = data.getDailyData();

        // 1. Mettre à jour les textes généraux
        binding.tvAvgKcal.setText(data.getAverageKcal() != null ? String.format(Locale.US, "%.0f KCAL / J", data.getAverageKcal()) : "0 KCAL");
        
        if (dailyData.size() >= 2) {
            double startWeight = dailyData.get(0).getWeightKg() != null ? dailyData.get(0).getWeightKg() : 0.0;
            double endWeight = dailyData.get(dailyData.size() - 1).getWeightKg() != null ? dailyData.get(dailyData.size() - 1).getWeightKg() : 0.0;
            double diff = endWeight - startWeight;
            String sign = diff > 0 ? "+" : "";
            binding.tvWeightDiff.setText(String.format(Locale.US, "%s%.1f KG", sign, diff));
        }

        // MAJ de la graisse estimée perdue
        if (data.getEstimatedFatLossGrams() != null && binding.tvFatLoss != null) {
            double fatLoss = data.getEstimatedFatLossGrams();
            if (fatLoss > 0) {
                binding.tvFatLoss.setText(String.format(Locale.US, "Graisse estimée perdue : %.0fg", fatLoss));
            } else if (fatLoss < 0) {
                binding.tvFatLoss.setText(String.format(Locale.US, "Graisse estimée gagnée : %.0fg", Math.abs(fatLoss)));
            } else {
                binding.tvFatLoss.setText("Graisse estimée perdue : 0g");
            }
        }

        // 2. BarChart Calories ET Ligne de Quota
        ArrayList<BarEntry> barEntriesKcal = new ArrayList<>();
        ArrayList<Entry> lineEntriesTarget = new ArrayList<>();
        ArrayList<String> dates = new ArrayList<>();
        
        int i = 0;
        for (AnalyticsResponse.DailyMetricPoint point : dailyData) {
            float kcal = point.getTotalKcal() != null ? point.getTotalKcal().floatValue() : 0f;
            barEntriesKcal.add(new BarEntry(i, kcal));
            
            // Pour la courbe cible : on prend le target du jour (calculé en back incluant le sport) ou le target par défaut du profil
            float target = 2500f; // Default fallback
            if (point.getTargetKcal() != null) {
                target = point.getTargetKcal().floatValue();
            } else if (currentUser != null && currentUser.getTargetKcal() != null) {
                target = currentUser.getTargetKcal().floatValue();
            }
            lineEntriesTarget.add(new Entry(i, target));
            
            // Format court "MM-dd" pour l'axe X
            try {
                Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(point.getDate());
                dates.add(new SimpleDateFormat("dd/MM", Locale.getDefault()).format(d));
            } catch (Exception e) {
                dates.add(String.valueOf(i));
            }
            i++;
        }
        
        BarDataSet barDataSetKcal = new BarDataSet(barEntriesKcal, "Consommées");
        barDataSetKcal.setColor(ContextCompat.getColor(requireContext(), R.color.primary_container)); // Primaire pour bien contraster
        barDataSetKcal.setValueTextColor(Color.WHITE);
        
        // On remplace le BarChart simple par un CombinedChart virtuellement en dessinant des barres et on pourrait dessiner une ligne...
        // Mais MPAndroidChart BarChart ne supporte pas nativement les lignes par dessus. 
        // L'astuce est d'utiliser un LimitLine ou d'utiliser un CombinedChart.
        // Puisque nous avons un BarChart dans le layout XML, on va ajouter la cible sous forme de LimitLine moyenne
        // OU on peut ajouter un LineDataSet secondaire si on le convertit en CombinedChart.
        // Pour faire simple et robuste sans changer le layout : on convertit le BarData classique.
        // Attendez, BarChart supporte-t-il LineDataSet ? Non. Il faut un CombinedChart.
        // A la place, je vais tracer la moyenne des cibles sous forme de LimitLine pour le BarChart existant.
        binding.barChartCalories.getAxisLeft().removeAllLimitLines(); // Clear previous
        
        if (!lineEntriesTarget.isEmpty()) {
            float avgTarget = 0;
            for(Entry e : lineEntriesTarget) avgTarget += e.getY();
            avgTarget /= lineEntriesTarget.size();
            
            com.github.mikephil.charting.components.LimitLine limitLine = new com.github.mikephil.charting.components.LimitLine(avgTarget, "Objectif Moyen");
            limitLine.setLineColor(ContextCompat.getColor(requireContext(), R.color.secondary));
            limitLine.setLineWidth(2f);
            limitLine.enableDashedLine(10f, 10f, 0f);
            limitLine.setTextColor(Color.WHITE);
            limitLine.setTextSize(10f);
            binding.barChartCalories.getAxisLeft().addLimitLine(limitLine);
        }

        BarData barData = new BarData(barDataSetKcal);
        barData.setBarWidth(0.5f);
        
        binding.barChartCalories.setData(barData);
        binding.barChartCalories.getXAxis().setValueFormatter(new IndexAxisValueFormatter(dates));
        binding.barChartCalories.getXAxis().setGranularity(1f); // One label per entry
        if(daysToFetch > 30) binding.barChartCalories.getXAxis().setLabelCount(5); 
        binding.barChartCalories.invalidate();

        // 3. PieChart Macros (Somme de toutes les macros de la période)
        float totalP = 0, totalC = 0, totalF = 0;
        for (AnalyticsResponse.DailyMetricPoint point : dailyData) {
            totalP += point.getTotalProteins() != null ? point.getTotalProteins() : 0;
            totalC += point.getTotalCarbs() != null ? point.getTotalCarbs() : 0;
            totalF += point.getTotalFats() != null ? point.getTotalFats() : 0;
        }

        ArrayList<PieEntry> pieEntries = new ArrayList<>();
        if (totalP > 0) pieEntries.add(new PieEntry(totalP, "Protéines"));
        if (totalC > 0) pieEntries.add(new PieEntry(totalC, "Glucides"));
        if (totalF > 0) pieEntries.add(new PieEntry(totalF, "Lipides"));

        PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
        pieDataSet.setColors(
                ContextCompat.getColor(requireContext(), R.color.primary_container),
                ContextCompat.getColor(requireContext(), R.color.secondary),
                Color.parseColor("#D81B60") // Custom red for Fats
        );
        pieDataSet.setValueTextColor(Color.WHITE);
        pieDataSet.setValueTextSize(14f);
        PieData pieData = new PieData(pieDataSet);
        binding.pieChartMacros.setData(pieData);
        binding.pieChartMacros.invalidate();

        // 4. LineChart Weight
        ArrayList<Entry> lineEntries = new ArrayList<>();
        int j = 0;
        for (AnalyticsResponse.DailyMetricPoint point : dailyData) {
            if (point.getWeightKg() != null && point.getWeightKg() > 0) {
                lineEntries.add(new Entry(j, point.getWeightKg().floatValue()));
            }
            j++;
        }

        if (!lineEntries.isEmpty()) {
            LineDataSet lineDataSet = new LineDataSet(lineEntries, "Poids");
            lineDataSet.setColor(Color.WHITE);
            lineDataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.secondary));
            lineDataSet.setLineWidth(2f);
            lineDataSet.setValueTextColor(Color.WHITE);
            
            LineData weightData = new LineData(lineDataSet);
            binding.lineChartWeight.setData(weightData);
            binding.lineChartWeight.getXAxis().setValueFormatter(new IndexAxisValueFormatter(dates));
            binding.lineChartWeight.getXAxis().setGranularity(1f);
            binding.lineChartWeight.invalidate();
        } else {
            binding.lineChartWeight.clear();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}