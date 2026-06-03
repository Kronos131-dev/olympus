package com.kronos.olympusfront;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kronos.olympusfront.databinding.ItemMealPresetBinding;
import com.kronos.olympusfront.network.dto.MealIngredientResponse;
import com.kronos.olympusfront.network.dto.MealPresetResponse;

import java.util.ArrayList;
import java.util.List;

public class MealsAdapter extends RecyclerView.Adapter<MealsAdapter.MealViewHolder> {

    private List<MealPresetResponse> meals = new ArrayList<>();
    private final List<MealPresetResponse> allMeals = new ArrayList<>();
    private final OnMealClickListener listener;
    
    // Variables pour les cibles (afin de configurer les max des ProgressBar)
    private int targetKcal = 2500;
    private int targetProteins = 180;
    private int targetCarbs = 350;
    private int targetFats = 85;

    public interface OnMealClickListener {
        void onConsumeClick(MealPresetResponse meal);
        void onEditClick(MealPresetResponse meal);
        void onDeleteClick(MealPresetResponse meal);
    }

    public MealsAdapter(OnMealClickListener listener) {
        this.listener = listener;
    }

    public void setMeals(List<MealPresetResponse> meals) {
        allMeals.clear();
        if (meals != null) {
            allMeals.addAll(meals);
        }
        this.meals = new ArrayList<>(allMeals);
        notifyDataSetChanged();
    }

    /** Filtre la liste affichée par nom de repas (recherche insensible à la casse). */
    public void filter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        if (q.isEmpty()) {
            this.meals = new ArrayList<>(allMeals);
        } else {
            List<MealPresetResponse> filtered = new ArrayList<>();
            for (MealPresetResponse meal : allMeals) {
                if (meal.getName() != null && meal.getName().toLowerCase().contains(q)) {
                    filtered.add(meal);
                }
            }
            this.meals = filtered;
        }
        notifyDataSetChanged();
    }
    
    // Permet de mettre à jour les cibles depuis le fragment
    public void setTargets(int kcal, int prot, int carbs, int fats) {
        this.targetKcal = kcal > 0 ? kcal : 2500;
        this.targetProteins = prot > 0 ? prot : 180;
        this.targetCarbs = carbs > 0 ? carbs : 350;
        this.targetFats = fats > 0 ? fats : 85;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMealPresetBinding binding = ItemMealPresetBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MealViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {
        MealPresetResponse meal = meals.get(position);
        holder.bind(meal);
    }

    @Override
    public int getItemCount() {
        return meals.size();
    }

    class MealViewHolder extends RecyclerView.ViewHolder {
        private final ItemMealPresetBinding binding;

        public MealViewHolder(@NonNull ItemMealPresetBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(MealPresetResponse meal) {
            binding.mealName.setText(meal.getName());
            
            // Affichage des ingrédients
            if (meal.getIngredients() != null && !meal.getIngredients().isEmpty()) {
                StringBuilder ingredientsStr = new StringBuilder();
                for (int i = 0; i < meal.getIngredients().size(); i++) {
                    MealIngredientResponse ingredient = meal.getIngredients().get(i);
                    String qty = ingredient.getQuantityGrams() != null ? String.valueOf(ingredient.getQuantityGrams().intValue()) : "0";
                    String name = ingredient.getFoodItem() != null ? ingredient.getFoodItem().getName() : "Inconnu";
                    
                    ingredientsStr.append(qty).append("g ").append(name);
                    if (i < meal.getIngredients().size() - 1) {
                        ingredientsStr.append(", ");
                    }
                }
                binding.mealIngredients.setText(ingredientsStr.toString());
                binding.mealIngredients.setVisibility(android.view.View.VISIBLE);
            } else {
                binding.mealIngredients.setVisibility(android.view.View.GONE);
            }
            
            int kcal = meal.getTotalKcal() != null ? meal.getTotalKcal().intValue() : 0;
            binding.mealCalories.setText("✦ " + kcal + " CAL");

            int protein = meal.getTotalProteins() != null ? meal.getTotalProteins().intValue() : 0;
            binding.mealProtein.setText(protein + "g");
            binding.progressProtein.setMax(targetProteins);
            binding.progressProtein.setProgress(protein);

            int carbs = meal.getTotalCarbs() != null ? meal.getTotalCarbs().intValue() : 0;
            binding.mealCarbs.setText(carbs + "g");
            binding.progressCarbs.setMax(targetCarbs);
            binding.progressCarbs.setProgress(carbs);

            int fats = meal.getTotalFats() != null ? meal.getTotalFats().intValue() : 0;
            binding.mealFats.setText(fats + "g");
            binding.progressFats.setMax(targetFats);
            binding.progressFats.setProgress(fats);

            binding.btnConsume.setOnClickListener(v -> listener.onConsumeClick(meal));
            binding.btnEditMeal.setOnClickListener(v -> listener.onEditClick(meal));
            binding.btnDeleteMeal.setOnClickListener(v -> listener.onDeleteClick(meal));
        }
    }
}