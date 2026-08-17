package com.kronos.olympus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/** Bilan des micronutriments d'une journée, un élément par nutriment suivi. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyMicronutrientsResponse {

    private LocalDate targetDate;

    @Builder.Default
    private List<MicronutrientResponse> nutrients = List.of();

    /** Couverture moyenne tous nutriments confondus, pour l'en-tête de l'écran. */
    private Double overallCoverage;
}
