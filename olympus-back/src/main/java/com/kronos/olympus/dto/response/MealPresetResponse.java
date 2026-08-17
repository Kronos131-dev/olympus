package com.kronos.olympus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPresetResponse {
    private Long id;
    private String name;
    private List<MealIngredientResponse> ingredients;
    // On peut aussi rajouter le total calculé de ce repas (somme des macros des ingrédients)
    private Double totalKcal;
    private Double totalProteins;
    private Double totalCarbs;
    private Double totalFats;

    // Palier A, en miroir de DailyLogResponse : fourni par CIQUAL et Open Food Facts, donc
    // totalisable de façon fiable même si un ingrédient vient d'un produit scanné.
    private Double totalFibers;
    private Double totalSugars;
    private Double totalSaturatedFat;
    private Double totalSalt;
}