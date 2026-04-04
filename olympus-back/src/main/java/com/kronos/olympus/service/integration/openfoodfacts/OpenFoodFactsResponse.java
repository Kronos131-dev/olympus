package com.kronos.olympus.service.integration.openfoodfacts;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class OpenFoodFactsResponse {
    private String code;
    private Integer status;
    @JsonProperty("status_verbose")
    private String statusVerbose;
    private OpenFoodFactsProduct product; // Pour la recherche par code-barres
    private List<OpenFoodFactsProduct> products; // Pour la recherche textuelle
}
