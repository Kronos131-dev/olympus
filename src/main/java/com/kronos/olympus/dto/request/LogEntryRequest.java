package com.kronos.olympus.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LogEntryRequest {

    @NotNull(message = "La date cible est obligatoire")
    private LocalDate targetDate;

    // Soit on donne un foodItemId, soit un mealPresetId
    private Long foodItemId;
    private Long mealPresetId;

    // Si on a fourni un foodItemId, il faut absolument une quantité.
    @Positive(message = "La quantité doit être supérieure à zéro")
    private Double quantityGrams;
}
