package com.kronos.olympus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealIngredientResponse {
    private Long id;
    private FoodItemResponse foodItem;
    private Double quantityGrams;
}
