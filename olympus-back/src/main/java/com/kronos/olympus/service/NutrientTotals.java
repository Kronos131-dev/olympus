package com.kronos.olympus.service;

import com.kronos.olympus.dto.response.MealPresetResponse;
import com.kronos.olympus.model.DailyLog;
import com.kronos.olympus.model.FoodItem;

/**
 * Accumulateur du palier A (kcal, macros, fibres, sucres, AG saturés, sel) à partir d'aliments et
 * de leur grammage. Partagé par {@link DailyLogService} (totaux du jour) et
 * {@link MealPresetTotalsCalculator} (totaux d'un repas pré-enregistré) : les deux faisaient
 * exactement la même mise à l'échelle « grammes / 100 », avec un arrondi qui ne différait que par
 * accident (1 décimale ici, 2 là).
 */
public class NutrientTotals {

    private double kcal;
    private double proteins;
    private double carbs;
    private double fats;
    private double fibers;
    private double sugars;
    private double saturatedFat;
    private double salt;

    public void add(FoodItem food, double grams) {
        double ratio = grams / 100.0;
        kcal += nz(food.getKcal100g()) * ratio;
        proteins += nz(food.getProteins100g()) * ratio;
        carbs += nz(food.getCarbs100g()) * ratio;
        fats += nz(food.getFats100g()) * ratio;
        // Une valeur absente compte pour zéro dans le total : c'est le taux de couverture,
        // calculé par MicronutrientService, qui dit à l'utilisateur ce que ce total ignore.
        fibers += nz(food.getFibers100g()) * ratio;
        sugars += nz(food.getSugars100g()) * ratio;
        saturatedFat += nz(food.getSaturatedFat100g()) * ratio;
        salt += nz(food.getSalt100g()) * ratio;
    }

    public void applyTo(DailyLog dailyLog) {
        dailyLog.setTotalKcal(round(kcal));
        dailyLog.setTotalProteins(round(proteins));
        dailyLog.setTotalCarbs(round(carbs));
        dailyLog.setTotalFats(round(fats));
        dailyLog.setTotalFibers(round(fibers));
        dailyLog.setTotalSugars(round(sugars));
        dailyLog.setTotalSaturatedFat(round(saturatedFat));
        dailyLog.setTotalSalt(round(salt));
    }

    public void applyTo(MealPresetResponse response) {
        response.setTotalKcal(round(kcal));
        response.setTotalProteins(round(proteins));
        response.setTotalCarbs(round(carbs));
        response.setTotalFats(round(fats));
        response.setTotalFibers(round(fibers));
        response.setTotalSugars(round(sugars));
        response.setTotalSaturatedFat(round(saturatedFat));
        response.setTotalSalt(round(salt));
    }

    private static double nz(Double value) {
        return value != null ? value : 0.0;
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
