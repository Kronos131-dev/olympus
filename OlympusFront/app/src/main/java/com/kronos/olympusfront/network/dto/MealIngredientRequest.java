package com.kronos.olympusfront.network.dto;

public class MealIngredientRequest {
    private Long foodItemId;
    private Double quantityGrams;

    public MealIngredientRequest() {}

    public MealIngredientRequest(Long foodItemId, Double quantityGrams) {
        this.foodItemId = foodItemId;
        this.quantityGrams = quantityGrams;
    }

    public Long getFoodItemId() { return foodItemId; }
    public void setFoodItemId(Long foodItemId) { this.foodItemId = foodItemId; }
    public Double getQuantityGrams() { return quantityGrams; }
    public void setQuantityGrams(Double quantityGrams) { this.quantityGrams = quantityGrams; }
}