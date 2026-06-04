package com.kronos.olympus.service;

import com.kronos.olympus.dto.response.MealPresetResponse;
import com.kronos.olympus.model.FoodItem;
import com.kronos.olympus.model.MealIngredient;
import com.kronos.olympus.model.MealPreset;
import org.springframework.stereotype.Component;

/**
 * Calcule les totaux nutritionnels (kcal + macros) d'un {@link MealPreset} à partir de ses
 * ingrédients. Centralisé ici car nécessaire à plusieurs endroits : la bibliothèque de repas
 * (MealPresetService) ET les entrées « repas » du journal (DailyLogService) — MapStruct ne
 * sait pas faire ce calcul, d'où les totaux à 0 quand on s'appuyait sur le mapper seul.
 */
@Component
public class MealPresetTotalsCalculator {

    public void applyTotals(MealPreset preset, MealPresetResponse response) {
        double totalKcal = 0;
        double totalProteins = 0;
        double totalCarbs = 0;
        double totalFats = 0;

        if (preset.getIngredients() != null) {
            for (MealIngredient ingredient : preset.getIngredients()) {
                if (ingredient.getFoodItem() != null && ingredient.getQuantityGrams() != null) {
                    double ratio = ingredient.getQuantityGrams() / 100.0;
                    FoodItem fi = ingredient.getFoodItem();

                    totalKcal += nz(fi.getKcal100g()) * ratio;
                    totalProteins += nz(fi.getProteins100g()) * ratio;
                    totalCarbs += nz(fi.getCarbs100g()) * ratio;
                    totalFats += nz(fi.getFats100g()) * ratio;
                }
            }
        }

        response.setTotalKcal(round1(totalKcal));
        response.setTotalProteins(round1(totalProteins));
        response.setTotalCarbs(round1(totalCarbs));
        response.setTotalFats(round1(totalFats));
    }

    private double nz(Double value) {
        return value != null ? value : 0.0;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
