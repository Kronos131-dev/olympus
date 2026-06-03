package com.kronos.olympusfront.network.dto;

public class PlannedMealEntryRequest {
    private Long foodItemId;
    private Long mealPresetId;
    private Double quantityGrams;
    private String plannedTime;
    private String dayOfWeek;

    public PlannedMealEntryRequest() {}

    public Long getFoodItemId() { return foodItemId; }
    public void setFoodItemId(Long foodItemId) { this.foodItemId = foodItemId; }
    public Long getMealPresetId() { return mealPresetId; }
    public void setMealPresetId(Long mealPresetId) { this.mealPresetId = mealPresetId; }
    public Double getQuantityGrams() { return quantityGrams; }
    public void setQuantityGrams(Double quantityGrams) { this.quantityGrams = quantityGrams; }
    public String getPlannedTime() { return plannedTime; }
    public void setPlannedTime(String plannedTime) { this.plannedTime = plannedTime; }
    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
}
