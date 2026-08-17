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

    // Palier A : fourni à la fois par CIQUAL et par Open Food Facts, donc fiable même pour un
    // produit scanné. Null si non déterminé pour cet aliment (pas 0 - voir FoodItem.fibers100g).
    private Double fibers100g;
    private Double sugars100g;
    private Double saturatedFat100g;
    private Double salt100g;

    private FoodSource source;
    private Double estimatedWeightGrams; // Utilisé par l'IA pour renvoyer le poids estimé
}
