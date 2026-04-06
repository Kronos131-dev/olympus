package com.kronos.olympusfront;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.kronos.olympusfront.databinding.ActivityCreateMealBinding;
import com.kronos.olympusfront.network.RetrofitClient;
import com.kronos.olympusfront.network.dto.FoodItemResponse;
import com.kronos.olympusfront.network.dto.MealIngredientRequest;
import com.kronos.olympusfront.network.dto.MealPresetRequest;
import com.kronos.olympusfront.network.dto.MealPresetResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateMealActivity extends AppCompatActivity {

    private static final String TAG = "CreateMealActivity";
    private ActivityCreateMealBinding binding;

    private FoodSearchAdapter searchAdapter;
    private SelectedIngredientsAdapter ingredientsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateMealBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        // Setup Search Recycler
        searchAdapter = new FoodSearchAdapter((foodItem, quantity) -> {
            ingredientsAdapter.addIngredient(new SelectedIngredientsAdapter.SelectedIngredient(foodItem, quantity));
            updateEmptyState();
            Toast.makeText(this, foodItem.getName() + " ajouté", Toast.LENGTH_SHORT).show();
            // Clear search results after adding
            searchAdapter.setFoodItems(new ArrayList<>());
            binding.etSearchFood.setText("");
        });
        binding.recyclerSearchResults.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerSearchResults.setAdapter(searchAdapter);

        // Setup Selected Ingredients Recycler
        ingredientsAdapter = new SelectedIngredientsAdapter(position -> {
            ingredientsAdapter.removeIngredient(position);
            updateEmptyState();
        });
        binding.recyclerSelectedIngredients.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerSelectedIngredients.setAdapter(ingredientsAdapter);
        updateEmptyState();

        // Search Button
        binding.btnSearch.setOnClickListener(v -> performSearch());

        // Save Button
        binding.btnSaveMeal.setOnClickListener(v -> saveMeal());
    }

    private void performSearch() {
        String query = binding.etSearchFood.getText().toString().trim();
        if (query.isEmpty()) return;

        RetrofitClient.getApiService().searchFoodItemsByName(query).enqueue(new Callback<List<FoodItemResponse>>() {
            @Override
            public void onResponse(Call<List<FoodItemResponse>> call, Response<List<FoodItemResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    searchAdapter.setFoodItems(response.body());
                } else {
                    Toast.makeText(CreateMealActivity.this, "Aucun résultat trouvé", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<FoodItemResponse>> call, Throwable t) {
                Log.e(TAG, "Search error", t);
                Toast.makeText(CreateMealActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveMeal() {
        String mealName = binding.etMealName.getText().toString().trim();
        if (mealName.isEmpty()) {
            Toast.makeText(this, "Veuillez donner un nom au repas", Toast.LENGTH_SHORT).show();
            return;
        }

        List<SelectedIngredientsAdapter.SelectedIngredient> selected = ingredientsAdapter.getIngredients();
        if (selected.isEmpty()) {
            Toast.makeText(this, "Ajoutez au moins un ingrédient", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSaveMeal.setEnabled(false);

        List<MealIngredientRequest> ingredientRequests = new ArrayList<>();
        for (SelectedIngredientsAdapter.SelectedIngredient item : selected) {
            ingredientRequests.add(new MealIngredientRequest(item.foodItem.getId(), item.quantityGrams));
        }

        MealPresetRequest request = new MealPresetRequest(mealName, ingredientRequests);

        RetrofitClient.getApiService().createMealPreset(request).enqueue(new Callback<MealPresetResponse>() {
            @Override
            public void onResponse(Call<MealPresetResponse> call, Response<MealPresetResponse> response) {
                binding.btnSaveMeal.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(CreateMealActivity.this, "Repas sauvegardé !", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Log.e(TAG, "Error saving meal: " + response.code());
                    Toast.makeText(CreateMealActivity.this, "Erreur lors de la sauvegarde", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MealPresetResponse> call, Throwable t) {
                binding.btnSaveMeal.setEnabled(true);
                Log.e(TAG, "Network error saving meal", t);
                Toast.makeText(CreateMealActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmptyState() {
        if (ingredientsAdapter.getItemCount() == 0) {
            binding.tvEmptyIngredients.setVisibility(View.VISIBLE);
            binding.recyclerSelectedIngredients.setVisibility(View.GONE);
        } else {
            binding.tvEmptyIngredients.setVisibility(View.GONE);
            binding.recyclerSelectedIngredients.setVisibility(View.VISIBLE);
        }
    }
}