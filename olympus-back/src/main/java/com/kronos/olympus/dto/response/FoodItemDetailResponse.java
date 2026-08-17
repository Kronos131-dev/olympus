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
public class FoodItemDetailResponse {
    private Long id;
    private String barcode;
    private String name;
    private FoodSource source;
    private String foodGroup;
    private String foodSubGroup;

    private Double kcal100g;
    private Double proteins100g;
    private Double carbs100g;
    private Double fats100g;

    private Double fibers100g;
    private Double sugars100g;
    private Double saturatedFat100g;
    private Double salt100g;

    @Builder.Default
    private Map<Nutrient, Double> micros100g = Map.of();
}
