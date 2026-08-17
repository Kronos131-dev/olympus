package com.kronos.olympus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un aliment tel qu'il figure dans l'analyse affichée à l'écran, renvoyé par le front pour une
 * correction ou une validation. Seuls le nom et la quantité circulent : les valeurs
 * nutritionnelles sont toujours recalculées côté serveur depuis le référentiel.
 */
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
