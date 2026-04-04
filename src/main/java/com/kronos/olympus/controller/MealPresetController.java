package com.kronos.olympus.controller;

import com.kronos.olympus.dto.request.MealPresetRequest;
import com.kronos.olympus.dto.response.MealPresetResponse;
import com.kronos.olympus.security.UserDetailsImpl;
import com.kronos.olympus.service.MealPresetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/meal-presets")
@RequiredArgsConstructor
public class MealPresetController {

    private final MealPresetService mealPresetService;

    // Créer un nouveau repas pré-enregistré (Preset)
    @PostMapping
    public ResponseEntity<MealPresetResponse> createMealPreset(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody MealPresetRequest request) {
        
        // On récupère l'utilisateur connecté depuis le contexte de sécurité via @AuthenticationPrincipal
        MealPresetResponse response = mealPresetService.createMealPreset(userDetails.getUser(), request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // Récupérer tous les repas pré-enregistrés de l'utilisateur connecté
    @GetMapping
    public ResponseEntity<List<MealPresetResponse>> getUserMealPresets(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        List<MealPresetResponse> responses = mealPresetService.getUserMealPresets(userDetails.getUser().getId());
        return ResponseEntity.ok(responses);
    }

    // Récupérer un repas spécifique
    @GetMapping("/{id}")
    public ResponseEntity<MealPresetResponse> getMealPresetById(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long id) {
        
        MealPresetResponse response = mealPresetService.getMealPresetById(id, userDetails.getUser().getId());
        return ResponseEntity.ok(response);
    }

    // Supprimer un repas pré-enregistré
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMealPreset(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long id) {
        
        mealPresetService.deleteMealPreset(id, userDetails.getUser().getId());
        return ResponseEntity.noContent().build();
    }
}
