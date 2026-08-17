package com.kronos.olympus.controller;

import com.kronos.olympus.dto.request.FoodItemRequest;
import com.kronos.olympus.dto.response.FoodItemDetailResponse;
import com.kronos.olympus.dto.response.FoodItemResponse;
import com.kronos.olympus.model.FoodItem;
import com.kronos.olympus.model.enums.FoodSource;
import com.kronos.olympus.repository.FoodItemRepository;
import com.kronos.olympus.service.FoodItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/food-items")
@RequiredArgsConstructor
public class FoodItemController {

    private final FoodItemService foodItemService;
    private final FoodItemRepository foodItemRepository;

    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<FoodItemResponse> getFoodItemByBarcode(@PathVariable String barcode) {
        FoodItemResponse response = foodItemService.getFoodItemByBarcode(barcode);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FoodItemDetailResponse> getFoodItemDetail(@PathVariable Long id) {
        return ResponseEntity.ok(foodItemService.getFoodItemDetail(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<FoodItemResponse>> searchFoodItemsByName(@RequestParam String query) {
        List<FoodItemResponse> responses = foodItemService.searchFoodItemsByName(query);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/search/ciqual")
    public ResponseEntity<List<FoodItemResponse>> searchCiqual(@RequestParam String query) {
        List<FoodItemResponse> responses = foodItemService.searchCiqualFoods(query);
        return ResponseEntity.ok(responses);
    }
    
    // Endpoint pour l'ajout d'un aliment 100% manuel
    @PostMapping("/manual")
    public ResponseEntity<FoodItemResponse> createManualFoodItem(@Valid @RequestBody FoodItemRequest request) {
        FoodItem foodItem = FoodItem.builder()
                .name(request.getName())
                .kcal100g(request.getKcal100g())
                .proteins100g(request.getProteins100g())
                .carbs100g(request.getCarbs100g())
                .fats100g(request.getFats100g())
                .source(FoodSource.MANUAL) // Ici c'est bien l'Enum
                .barcode("MAN-" + UUID.randomUUID().toString().substring(0, 8))
                .build();
                
        FoodItem saved = foodItemRepository.save(foodItem);
        
        FoodItemResponse response = FoodItemResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .kcal100g(saved.getKcal100g())
                .proteins100g(saved.getProteins100g())
                .carbs100g(saved.getCarbs100g())
                .fats100g(saved.getFats100g())
                .source(saved.getSource()) // C'est un Enum dans FoodItemResponse aussi
                .build();
                
        return ResponseEntity.ok(response);
    }
}