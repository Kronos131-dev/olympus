package com.kronos.olympus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzedFoodRequest {
    @NotBlank(message = "Le nom de l'aliment est obligatoire")
    private String name;

    @Positive(message = "La quantité doit être positive")
    private Double quantityGrams;

    private Long foodItemId;
}
