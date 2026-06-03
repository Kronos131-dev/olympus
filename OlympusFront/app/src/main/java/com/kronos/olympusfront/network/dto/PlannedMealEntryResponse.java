package com.kronos.olympusfront.network.dto;

import java.io.Serializable;

public class PlannedMealEntryResponse implements Serializable {
    private Long id;
    private FoodItemResponse foodItem;
    private MealPresetResponse mealPreset;
    private Double quantityGrams;
    private String plannedTime;
    private String dayOfWeek;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public FoodItemResponse getFoodItem() { return foodItem; }
    public void setFoodItem(FoodItemResponse foodItem) { this.foodItem = foodItem; }
    public MealPresetResponse getMealPreset() { return mealPreset; }
    public void setMealPreset(MealPresetResponse mealPreset) { this.mealPreset = mealPreset; }
    public Double getQuantityGrams() { return quantityGrams; }
    public void setQuantityGrams(Double quantityGrams) { this.quantityGrams = quantityGrams; }
    public String getPlannedTime() { return plannedTime; }
    public void setPlannedTime(String plannedTime) { this.plannedTime = plannedTime; }
    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
}
