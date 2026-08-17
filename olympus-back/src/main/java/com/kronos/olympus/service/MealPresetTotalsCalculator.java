package com.kronos.olympus.service;

import com.kronos.olympus.dto.response.MealPresetResponse;
import com.kronos.olympus.model.MealIngredient;
import com.kronos.olympus.model.MealPreset;
import org.springframework.stereotype.Component;

/**
 * Calcule les totaux nutritionnels (palier A complet) d'un {@link MealPreset} à partir de ses
 * ingrédients. Centralisé ici car nécessaire à plusieurs endroits : la bibliothèque de repas
 * (MealPresetService), les entrées « repas » du journal (DailyLogService) et le planning
 * (MealPlanService) — MapStruct ne sait pas faire ce calcul, d'où les totaux à 0 quand on
 * s'appuyait sur le mapper seul.
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
