package com.kronos.olympus.dto.response;

import com.kronos.olympus.model.enums.FoodSource;
import com.kronos.olympus.model.enums.Nutrient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzedFoodResponse {
    private String name;
    private Double quantityGrams;
    private FoodSource source;

    private Long foodItemId;

    private Double kcal;
    private Double proteins;
    private Double carbs;
    private Double fats;

    private Double fibers;
    private Double sugars;
    private Double saturatedFat;
    private Double salt;

    @Builder.Default
    private Map<Nutrient, Double> micros = Map.of();
}
