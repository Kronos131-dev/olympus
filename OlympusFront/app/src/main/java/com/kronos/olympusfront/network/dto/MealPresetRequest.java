package com.kronos.olympusfront.network.dto;

import java.util.List;

public class MealPresetRequest {
    private String name;
    private List<MealIngredientRequest> ingredients;

    public MealPresetRequest() {}

    public MealPresetRequest(String name, List<MealIngredientRequest> ingredients) {
        this.name = name;
        this.ingredients = ingredients;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<MealIngredientRequest> getIngredients() { return ingredients; }
    public void setIngredients(List<MealIngredientRequest> ingredients) { this.ingredients = ingredients; }
}