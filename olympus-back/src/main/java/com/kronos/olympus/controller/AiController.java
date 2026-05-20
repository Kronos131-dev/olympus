package com.kronos.olympus.controller;

import com.kronos.olympus.dto.request.AiMealRequest;
import com.kronos.olympus.dto.response.AiEstimation;
import com.kronos.olympus.dto.response.FoodItemResponse;
import com.kronos.olympus.model.FoodItem;
import com.kronos.olympus.model.enums.FoodSource;
import com.kronos.olympus.repository.FoodItemRepository;
import com.kronos.olympus.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final FoodItemRepository foodItemRepository;

    @PostMapping("/analyze-meal")
    public ResponseEntity<FoodItemResponse> analyzeMealAndSave(@Valid @RequestBody AiMealRequest request) {
        
        // L'IA analyse le texte et nous renvoie une estimation totale du repas
        AiEstimation estimation = aiService.analyzeMeal(request.getDescription());
        
        // On convertit les totaux en "valeurs pour 100g" pour que ça colle à la structure FoodItem.
        // Si l'IA estime que le repas fait 500g et contient 1000 Kcal, on va stocker 200 Kcal/100g.
        // Comme ça, dans l'application, quand l'utilisateur cliquera sur "Ajouter", on lui proposera par défaut le poids total (ex: 500g).
        
        double weightRatio = estimation.getTotalWeightGrams() > 0 ? (100.0 / estimation.getTotalWeightGrams()) : 1.0;
        
        FoodItem foodItem = FoodItem.builder()
                .name("IA: " + estimation.getName())
                .kcal100g(Math.round(estimation.getTotalKcal() * weightRatio * 10.0) / 10.0)
                .proteins100g(Math.round(estimation.getTotalProteins() * weightRatio * 10.0) / 10.0)
                .carbs100g(Math.round(estimation.getTotalCarbs() * weightRatio * 10.0) / 10.0)
                .fats100g(Math.round(estimation.getTotalFats() * weightRatio * 10.0) / 10.0)
                .source(FoodSource.AI) 
                .barcode("AI-" + UUID.randomUUID().toString().substring(0, 8))
                .build();
                
        FoodItem saved = foodItemRepository.save(foodItem);
        
        FoodItemResponse response = FoodItemResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .kcal100g(saved.getKcal100g())
                .proteins100g(saved.getProteins100g())
                .carbs100g(saved.getCarbs100g())
                .fats100g(saved.getFats100g())
                .source(saved.getSource())
                .estimatedWeightGrams(estimation.getTotalWeightGrams()) // On passe le poids estimé au front pour pré-remplir la quantité !
                .build();
                
        return ResponseEntity.ok(response);
    }
}