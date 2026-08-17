package com.kronos.olympus.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/** Validation d'une analyse : chaque aliment retenu devient une entrée du journal du jour. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealConfirmationRequest {

    private LocalDate targetDate;

    @NotEmpty(message = "Aucun aliment à enregistrer")
    private List<AnalyzedFoodRequest> items;
}
