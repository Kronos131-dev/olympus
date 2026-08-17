package com.kronos.olympus.service;

import com.kronos.olympus.dto.response.MealPresetResponse;
import com.kronos.olympus.model.DailyLog;
import com.kronos.olympus.model.FoodItem;

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
