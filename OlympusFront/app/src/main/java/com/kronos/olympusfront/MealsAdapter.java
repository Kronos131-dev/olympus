package com.kronos.olympusfront;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kronos.olympusfront.databinding.ItemMealPresetBinding;
import com.kronos.olympusfront.network.dto.MealPresetResponse;

import java.util.ArrayList;
import java.util.List;

public class MealsAdapter extends RecyclerView.Adapter<MealsAdapter.MealViewHolder> {

    private List<MealPresetResponse> meals = new ArrayList<>();
    private final OnMealClickListener listener;

    public interface OnMealClickListener {
        void onConsumeClick(MealPresetResponse meal);
    }

    public MealsAdapter(OnMealClickListener listener) {
        this.listener = listener;
    }

    public void setMeals(List<MealPresetResponse> meals) {
        this.meals = meals;
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
            
            int kcal = meal.getTotalKcal() != null ? meal.getTotalKcal().intValue() : 0;
            binding.mealCalories.setText("✦ " + kcal + " CAL");

            int protein = meal.getTotalProteins() != null ? meal.getTotalProteins().intValue() : 0;
            binding.mealProtein.setText(protein + "g");
            binding.progressProtein.setProgress(protein);

            int carbs = meal.getTotalCarbs() != null ? meal.getTotalCarbs().intValue() : 0;
            binding.mealCarbs.setText(carbs + "g");
            binding.progressCarbs.setProgress(carbs);

            int fats = meal.getTotalFats() != null ? meal.getTotalFats().intValue() : 0;
            binding.mealFats.setText(fats + "g");
            binding.progressFats.setProgress(fats);

            binding.btnConsume.setOnClickListener(v -> listener.onConsumeClick(meal));
        }
    }
}