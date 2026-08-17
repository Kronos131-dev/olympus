package com.kronos.olympus.service;

import com.kronos.olympus.dto.response.MealPresetResponse;
import com.kronos.olympus.model.MealIngredient;
import com.kronos.olympus.model.MealPreset;
import org.springframework.stereotype.Component;

/**
 * ingrédients. Centralisé ici car nécessaire à plusieurs endroits : la bibliothèque de repas
 */
@Component
public class MealPresetTotalsCalculator {

    public void applyTotals(MealPreset preset, MealPresetResponse response) {
        NutrientTotals totals = new NutrientTotals();

        if (preset.getIngredients() != null) {
            for (MealIngredient ingredient : preset.getIngredients()) {
                if (ingredient.getFoodItem() != null && ingredient.getQuantityGrams() != null) {
                    totals.add(ingredient.getFoodItem(), ingredient.getQuantityGrams());
                }
            }
        }

        totals.applyTo(response);
    }
}
