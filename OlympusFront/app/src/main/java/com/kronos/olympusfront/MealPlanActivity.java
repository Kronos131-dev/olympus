package com.kronos.olympusfront;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.gson.Gson;
import com.kronos.olympusfront.databinding.ActivityMealPlanBinding;
import com.kronos.olympusfront.network.RetrofitClient;
import com.kronos.olympusfront.network.dto.MealPlanResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MealPlanActivity extends AppCompatActivity implements MealPlanAdapter.OnPlanActionListener {

    private static final String TAG = "MealPlanActivity";
    public static final String EXTRA_MEAL_PLAN = "EXTRA_MEAL_PLAN";

    private ActivityMealPlanBinding binding;
    private MealPlanAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMealPlanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adapter = new MealPlanAdapter(this);
        binding.recyclerPlans.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerPlans.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnCreatePlan.setOnClickListener(v ->
                startActivity(new Intent(this, CreateMealPlanActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchPlans();
    }

    private void fetchPlans() {
        RetrofitClient.getApiService().getMealPlans().enqueue(new Callback<List<MealPlanResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<MealPlanResponse>> call,
                                   @NonNull Response<List<MealPlanResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setPlans(response.body());
                    binding.emptyView.setVisibility(response.body().isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    Log.e(TAG, "Erreur récupération plans: " + response.code());
                    Toast.makeText(MealPlanActivity.this, "Erreur serveur: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<MealPlanResponse>> call, @NonNull Throwable t) {
                Log.e(TAG, "Erreur réseau (plans)", t);
                Toast.makeText(MealPlanActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onEditPlan(MealPlanResponse plan) {
        Intent intent = new Intent(this, CreateMealPlanActivity.class);
        intent.putExtra(EXTRA_MEAL_PLAN, new Gson().toJson(plan));
        startActivity(intent);
    }

    @Override
    public void onDeletePlan(MealPlanResponse plan) {
        new AlertDialog.Builder(this, R.style.OlympusAlertDialogTheme)
                .setTitle("Supprimer ce plan ?")
                .setMessage("Voulez-vous vraiment supprimer le plan '" + plan.getName() + "' ?")
                .setPositiveButton("Supprimer", (dialog, which) ->
                        RetrofitClient.getApiService().deleteMealPlan(plan.getId()).enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(MealPlanActivity.this, "Plan supprimé", Toast.LENGTH_SHORT).show();
                                    fetchPlans();
                                } else {
                                    Toast.makeText(MealPlanActivity.this, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                                Toast.makeText(MealPlanActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show();
                            }
                        }))
                .setNegativeButton("Annuler", null)
                .show();
    }
}
