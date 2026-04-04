package com.kronos.olympus.controller;

import com.kronos.olympus.dto.response.FoodItemResponse;
import com.kronos.olympus.service.FoodItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/food-items")
@RequiredArgsConstructor
public class FoodItemController {

    private final FoodItemService foodItemService;

    // Endpoint pour la recherche via le scan du code-barres (le fameux fallback local/externe)
    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<FoodItemResponse> getFoodItemByBarcode(@PathVariable String barcode) {
        FoodItemResponse response = foodItemService.getFoodItemByBarcode(barcode);
        return ResponseEntity.ok(response);
    }

    // Endpoint pour la recherche textuelle (pour la saisie manuelle sans code-barres)
    @GetMapping("/search")
    public ResponseEntity<List<FoodItemResponse>> searchFoodItemsByName(@RequestParam String query) {
        List<FoodItemResponse> responses = foodItemService.searchFoodItemsByName(query);
        return ResponseEntity.ok(responses);
    }
}
