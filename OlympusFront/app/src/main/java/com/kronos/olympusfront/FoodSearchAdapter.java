package com.kronos.olympusfront;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kronos.olympusfront.databinding.ItemFoodSearchBinding;
import com.kronos.olympusfront.network.dto.FoodItemResponse;

import java.util.ArrayList;
import java.util.List;

public class FoodSearchAdapter extends RecyclerView.Adapter<FoodSearchAdapter.FoodViewHolder> {

    private List<FoodItemResponse> foodItems = new ArrayList<>();
    private final OnAddIngredientListener listener;

    public interface OnAddIngredientListener {
        void onAddIngredient(FoodItemResponse foodItem, double quantity);
    }

    public FoodSearchAdapter(OnAddIngredientListener listener) {
        this.listener = listener;
    }

    public void setFoodItems(List<FoodItemResponse> foodItems) {
        this.foodItems = foodItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFoodSearchBinding binding = ItemFoodSearchBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new FoodViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodViewHolder holder, int position) {
        holder.bind(foodItems.get(position));
    }

    @Override
    public int getItemCount() {
        return foodItems.size();
    }

    class FoodViewHolder extends RecyclerView.ViewHolder {
        private final ItemFoodSearchBinding binding;

        public FoodViewHolder(@NonNull ItemFoodSearchBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(FoodItemResponse foodItem) {
            binding.foodName.setText(foodItem.getName());
            binding.foodSource.setText("SOURCE: " + foodItem.getSource());
            binding.foodMacros.setText(foodItem.getKcal100g() + " KCAL\n(100g)");

            binding.btnAddIngredient.setOnClickListener(v -> {
                String qtyStr = binding.etQuantity.getText().toString();
                if (!qtyStr.isEmpty()) {
                    double qty = Double.parseDouble(qtyStr);
                    listener.onAddIngredient(foodItem, qty);
                    binding.etQuantity.setText(""); // Clear after adding
                } else {
                    binding.etQuantity.setError("!");
                }
            });
        }
    }
}