package com.kronos.olympusfront;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;

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
            
            // Si l'aliment vient de l'IA et a un poids estimé, on pré-remplit avec ce poids pour avoir le total
            if ("AI".equals(foodItem.getSource()) && foodItem.getEstimatedWeightGrams() != null) {
                 binding.etQuantity.setText(String.valueOf(foodItem.getEstimatedWeightGrams().intValue()));
            } else {
                 binding.etQuantity.setText(""); // Vidé par défaut pour Ciqual et autres
            }

            // Gestion du clic classique sur le bouton
            binding.btnAddIngredient.setOnClickListener(v -> addIngredientAndHideKeyboard(foodItem, v));

            // Gestion de l'action "Terminé" (Enter / Done) sur le clavier
            binding.etQuantity.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE || 
                    (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    addIngredientAndHideKeyboard(foodItem, v);
                    return true; // L'événement est consommé
                }
                return false;
            });
        }

        private void addIngredientAndHideKeyboard(FoodItemResponse foodItem, View view) {
            String qtyStr = binding.etQuantity.getText().toString();
            if (!qtyStr.isEmpty()) {
                double qty = Double.parseDouble(qtyStr);
                listener.onAddIngredient(foodItem, qty);
                binding.etQuantity.setText(""); // Clear after adding
                
                // Masquer le clavier virtuel
                InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            } else {
                binding.etQuantity.setError("!");
            }
        }
    }
}