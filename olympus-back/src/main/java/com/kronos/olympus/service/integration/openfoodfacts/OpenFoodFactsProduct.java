package com.kronos.olympus.service.integration.openfoodfacts;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class OpenFoodFactsProduct {
    private String code;
    private String productName;
    private String genericName;
    private String quantity; // Ex: "500g"
    private String brands;

    @JsonProperty("nutriments")
    private Nutriments nutriments;

    @Data
    @NoArgsConstructor
    public static class Nutriments {
        @JsonProperty("energy-kcal_100g")
        private Double energyKcal100g;

        @JsonProperty("proteins_100g")
        private Double proteins100g;

        @JsonProperty("carbohydrates_100g")
        private Double carbohydrates100g;

        @JsonProperty("fat_100g")
        private Double fat100g;
    }
}
