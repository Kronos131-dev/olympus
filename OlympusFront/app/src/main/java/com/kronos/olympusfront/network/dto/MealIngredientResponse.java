package com.kronos.olympusfront.network.dto;

import java.io.Serializable;

public class MealIngredientResponse implements Serializable {
    private Long id;
    private FoodItemResponse foodItem;
    private Double quantityGrams;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public FoodItemResponse getFoodItem() { return foodItem; }
    public void setFoodItem(FoodItemResponse foodItem) { this.foodItem = foodItem; }
    public Double getQuantityGrams() { return quantityGrams; }
    public void setQuantityGrams(Double quantityGrams) { this.quantityGrams = quantityGrams; }
}