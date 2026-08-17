package com.kronos.olympus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealCorrectionRequest {
    @NotBlank(message = "La correction ne peut pas être vide")
    private String correction;

    private String mealName;

    @NotNull(message = "L'analyse à corriger est obligatoire")
    private List<AnalyzedFoodRequest> items;
}
