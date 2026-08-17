package com.kronos.olympus.dto.response;

import com.kronos.olympus.model.enums.Nutrient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

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

    private Double microCoverage;
}
