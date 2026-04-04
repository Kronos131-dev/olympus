package com.kronos.olympus.service.integration;

import com.kronos.olympus.exception.ExternalApiException;
import com.kronos.olympus.model.FoodItem;
import com.kronos.olympus.model.enums.FoodSource;
import com.kronos.olympus.service.integration.openfoodfacts.OpenFoodFactsProduct;
import com.kronos.olympus.service.integration.openfoodfacts.OpenFoodFactsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenFoodFactsClient {

    private final RestTemplate restTemplate;

    @Value("${application.integration.openfoodfacts.api-url:https://world.openfoodfacts.org/api/v2}")
    private String apiUrl;

    public Optional<FoodItem> fetchProductByBarcode(String barcode) {
        String url = apiUrl + "/product/" + barcode + ".json";

        try {
            ResponseEntity<OpenFoodFactsResponse> responseEntity = restTemplate.getForEntity(url, OpenFoodFactsResponse.class);
            OpenFoodFactsResponse responseBody = responseEntity.getBody();

            if (responseBody != null && responseBody.getStatus() != null && responseBody.getStatus() == 1 && responseBody.getProduct() != null) {
                return Optional.of(mapToFoodItem(responseBody.getProduct()));
            } else {
                log.warn("Produit non trouvé sur Open Food Facts pour le code-barres : {}", barcode);
                return Optional.empty();
            }

        } catch (RestClientException e) {
            log.error("Erreur lors de l'appel à l'API Open Food Facts pour le code-barres {}: {}", barcode, e.getMessage());
            throw new ExternalApiException("Erreur lors de la communication avec le service Open Food Facts");
        }
    }

    public List<FoodItem> searchProductsByName(String name) {
        String url = apiUrl + "/search?search_terms=" + name + "&search_simple=1&action=process&json=1&page_size=10";

        try {
            ResponseEntity<OpenFoodFactsResponse> responseEntity = restTemplate.getForEntity(url, OpenFoodFactsResponse.class);
            OpenFoodFactsResponse responseBody = responseEntity.getBody();

            List<FoodItem> foodItems = new ArrayList<>();
            if (responseBody != null && responseBody.getProducts() != null) {
                for (OpenFoodFactsProduct product : responseBody.getProducts()) {
                    if (isValidProduct(product)) { // On ne garde que ceux avec les macros
                        foodItems.add(mapToFoodItem(product));
                    }
                }
            }
            return foodItems;

        } catch (RestClientException e) {
            log.error("Erreur lors de la recherche sur Open Food Facts pour '{}': {}", name, e.getMessage());
            throw new ExternalApiException("Erreur lors de la recherche sur le service Open Food Facts");
        }
    }

    private boolean isValidProduct(OpenFoodFactsProduct product) {
        return product != null &&
               product.getProductName() != null && !product.getProductName().isEmpty() &&
               product.getNutriments() != null &&
               product.getNutriments().getEnergyKcal100g() != null &&
               product.getNutriments().getProteins100g() != null &&
               product.getNutriments().getCarbohydrates100g() != null &&
               product.getNutriments().getFat100g() != null;
    }

    private FoodItem mapToFoodItem(OpenFoodFactsProduct product) {
        // Fallback name if product_name is null but brands or generic_name is available
        String name = product.getProductName() != null && !product.getProductName().isEmpty() ? product.getProductName() : (product.getGenericName() != null ? product.getGenericName() : "Produit Inconnu");
        
        if (product.getBrands() != null && !product.getBrands().isEmpty()) {
            name = product.getBrands() + " - " + name;
        }

        return FoodItem.builder()
                .barcode(product.getCode()) // L'API renvoie le code
                .name(name)
                .kcal100g(product.getNutriments().getEnergyKcal100g() != null ? product.getNutriments().getEnergyKcal100g() : 0.0)
                .proteins100g(product.getNutriments().getProteins100g() != null ? product.getNutriments().getProteins100g() : 0.0)
                .carbs100g(product.getNutriments().getCarbohydrates100g() != null ? product.getNutriments().getCarbohydrates100g() : 0.0)
                .fats100g(product.getNutriments().getFat100g() != null ? product.getNutriments().getFat100g() : 0.0)
                .source(FoodSource.OFF)
                .build();
    }
}
