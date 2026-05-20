package com.kronos.olympus.dto.response;

import com.kronos.olympus.model.enums.FoodSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodItemResponse {
    private Long id;
    private String barcode;
    private String name;
    private Double kcal100g;
    private Double proteins100g;
    private Double carbs100g;
    private Double fats100g;
    private FoodSource source;
    private Double estimatedWeightGrams; // Utilisé par l'IA pour renvoyer le poids estimé
}
