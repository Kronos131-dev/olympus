package com.kronos.olympus.service.integration.openfoodfacts;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class OpenFoodFactsProduct {
    private String code;

    // WHY: l'API Open Food Facts renvoie ces clés en snake_case ; sans @JsonProperty,
    // Jackson les mappe sur productName/genericName (camelCase) qui n'existent jamais dans
    // la réponse, et les deux champs restent null — d'où "Produit Inconnu" au scan et une
    // recherche texte qui ne renvoie jamais rien (isValidProduct exige un productName).
    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("generic_name")
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

        @JsonProperty("fiber_100g")
        private Double fiber100g;

        @JsonProperty("sugars_100g")
        private Double sugars100g;

        @JsonProperty("saturated-fat_100g")
        private Double saturatedFat100g;

        @JsonProperty("salt_100g")
        private Double salt100g;
    }
}
