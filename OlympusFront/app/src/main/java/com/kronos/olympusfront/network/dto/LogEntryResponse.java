package com.kronos.olympusfront.network.dto;

public class LogEntryResponse {
    private Long id;
    private FoodItemResponse foodItem;
    private MealPresetResponse mealPreset;
    private Double quantityGrams;
    private String consumedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public FoodItemResponse getFoodItem() { return foodItem; }
    public void setFoodItem(FoodItemResponse foodItem) { this.foodItem = foodItem; }
    public MealPresetResponse getMealPreset() { return mealPreset; }
    public void setMealPreset(MealPresetResponse mealPreset) { this.mealPreset = mealPreset; }
    public Double getQuantityGrams() { return quantityGrams; }
    public void setQuantityGrams(Double quantityGrams) { this.quantityGrams = quantityGrams; }
    public String getConsumedAt() { return consumedAt; }
    public void setConsumedAt(String consumedAt) { this.consumedAt = consumedAt; }
}