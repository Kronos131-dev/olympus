package com.kronos.olympus.dto.response;

import com.kronos.olympus.model.enums.Nutrient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/** Résultat d'une analyse de repas : le détail par aliment, puis les totaux du repas. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealAnalysisResponse {

    private String mealName;

    @Builder.Default
    private List<AnalyzedFoodResponse> items = List.of();

    private Double totalKcal;
    private Double totalProteins;
    private Double totalCarbs;
    private Double totalFats;
    private Double totalFibers;
    private Double totalSugars;
    private Double totalSaturatedFat;
    private Double totalSalt;

    @Builder.Default
    private Map<Nutrient, Double> micros = Map.of();

    /**
     * Part des calories du repas provenant d'aliments dont les micronutriments sont connus, entre
     * 0 et 1. Un repas entièrement estimé par l'IA vaut 0 : les micros affichés ne veulent alors
     * rien dire, et l'écran doit le signaler au lieu de les présenter comme des carences.
     */
    private Double microCoverage;
}
