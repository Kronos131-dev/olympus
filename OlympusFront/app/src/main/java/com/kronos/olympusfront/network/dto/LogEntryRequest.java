package com.kronos.olympusfront.network.dto;

public class LogEntryRequest {
    private String targetDate;
    private Long foodItemId;
    private Long mealPresetId;
    private Double quantityGrams;

    public LogEntryRequest() {}

    public LogEntryRequest(String targetDate, Long foodItemId, Long mealPresetId, Double quantityGrams) {
        this.targetDate = targetDate;
        this.foodItemId = foodItemId;
        this.mealPresetId = mealPresetId;
        this.quantityGrams = quantityGrams;
    }

    public String getTargetDate() { return targetDate; }
    public void setTargetDate(String targetDate) { this.targetDate = targetDate; }
    public Long getFoodItemId() { return foodItemId; }
    public void setFoodItemId(Long foodItemId) { this.foodItemId = foodItemId; }
    public Long getMealPresetId() { return mealPresetId; }
    public void setMealPresetId(Long mealPresetId) { this.mealPresetId = mealPresetId; }
    public Double getQuantityGrams() { return quantityGrams; }
    public void setQuantityGrams(Double quantityGrams) { this.quantityGrams = quantityGrams; }
}