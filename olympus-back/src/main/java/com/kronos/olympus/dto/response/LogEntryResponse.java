package com.kronos.olympus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEntryResponse {
    private Long id;
    
    // Soit l'un soit l'autre
    private FoodItemResponse foodItem;
    private MealPresetResponse mealPreset;
    
    private Double quantityGrams;
    private String unit;
    private Double amount;
    private LocalDateTime consumedAt;

    // True si l'entrée a été générée automatiquement depuis un plan de repas
    private Boolean fromPlan;
}
