package com.kronos.olympusfront;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kronos.olympusfront.databinding.ItemSelectedIngredientBinding;
import com.kronos.olympusfront.network.dto.FoodItemResponse;

import java.util.ArrayList;
import java.util.List;

public class SelectedIngredientsAdapter extends RecyclerView.Adapter<SelectedIngredientsAdapter.IngredientViewHolder> {

    // Simple class to hold pair
    public static class SelectedIngredient {
        public FoodItemResponse foodItem;
        public double quantityGrams;

        public SelectedIngredient(FoodItemResponse foodItem, double quantityGrams) {
            this.foodItem = foodItem;
            this.quantityGrams = quantityGrams;
        }
    }

    private List<SelectedIngredient> ingredients = new ArrayList<>();
    private final OnRemoveIngredientListener listener;

    public interface OnRemoveIngredientListener {
        void onRemoveIngredient(int position);
    }

    public SelectedIngredientsAdapter(OnRemoveIngredientListener listener) {
        this.listener = listener;
    }

    public void setIngredients(List<SelectedIngredient> ingredients) {
        this.ingredients = ingredients;
        notifyDataSetChanged();
    }

    public void addIngredient(SelectedIngredient ingredient) {
        this.ingredients.add(ingredient);
        notifyItemInserted(this.ingredients.size() - 1);
    }
    
    public void removeIngredient(int position) {
        this.ingredients.remove(position);
        notifyItemRemoved(position);
    }

    public List<SelectedIngredient> getIngredients() {
        return ingredients;
    }

    @NonNull
    @Override
    public IngredientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSelectedIngredientBinding binding = ItemSelectedIngredientBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new IngredientViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull IngredientViewHolder holder, int position) {
        holder.bind(ingredients.get(position), position);
    }

    @Override
    public int getItemCount() {
        return ingredients.size();
    }

    class IngredientViewHolder extends RecyclerView.ViewHolder {
        private final ItemSelectedIngredientBinding binding;

        public IngredientViewHolder(@NonNull ItemSelectedIngredientBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(SelectedIngredient ingredient, int position) {
            binding.ingredientName.setText(ingredient.foodItem.getName());
            binding.ingredientQuantity.setText(ingredient.quantityGrams + "g");

            binding.btnRemove.setOnClickListener(v -> listener.onRemoveIngredient(getAdapterPosition()));
        }
    }
}