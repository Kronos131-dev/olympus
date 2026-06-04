package com.kronos.olympus.service;

import com.kronos.olympus.dto.request.MealPresetRequest;
import com.kronos.olympus.dto.response.MealPresetResponse;
import com.kronos.olympus.exception.EntityNotFoundException;
import com.kronos.olympus.mapper.MealPresetMapper;
import com.kronos.olympus.model.FoodItem;
import com.kronos.olympus.model.MealIngredient;
import com.kronos.olympus.model.MealPreset;
import com.kronos.olympus.model.User;
import com.kronos.olympus.repository.FoodItemRepository;
import com.kronos.olympus.repository.LogEntryRepository;
import com.kronos.olympus.repository.MealIngredientRepository;
import com.kronos.olympus.repository.MealPresetRepository;
import com.kronos.olympus.repository.PlannedMealEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MealPresetService {

    private final MealPresetRepository mealPresetRepository;
    private final FoodItemRepository foodItemRepository;
    private final MealIngredientRepository mealIngredientRepository;
    private final LogEntryRepository logEntryRepository;
    private final PlannedMealEntryRepository plannedMealEntryRepository;
    private final MealPresetMapper mealPresetMapper;
    private final MealPresetTotalsCalculator totalsCalculator;

    @Transactional
    public MealPresetResponse createMealPreset(User user, MealPresetRequest request) {
        log.info("Création d'un nouveau repas pré-enregistré '{}' pour l'utilisateur ID: {}", request.getName(), user.getId());

        MealPreset mealPreset = MealPreset.builder()
                .user(user)
                .name(request.getName())
                .build();

        List<MealIngredient> ingredients = request.getIngredients().stream().map(ingReq -> {
            FoodItem foodItem = foodItemRepository.findById(ingReq.getFoodItemId())
                    .orElseThrow(() -> new EntityNotFoundException("Aliment introuvable avec l'ID: " + ingReq.getFoodItemId()));
            
            return MealIngredient.builder()
                    .mealPreset(mealPreset)
                    .foodItem(foodItem)
                    .quantityGrams(ingReq.getQuantityGrams())
                    .build();
        }).collect(Collectors.toList());

        mealPreset.setIngredients(ingredients);
        MealPreset savedPreset = mealPresetRepository.save(mealPreset);
        
        return mapToResponseWithTotals(savedPreset);
    }

    @Transactional
    public MealPresetResponse updateMealPreset(User user, Long presetId, MealPresetRequest request) {
        log.info("Mise à jour du repas pré-enregistré '{}' pour l'utilisateur ID: {}", request.getName(), user.getId());

        MealPreset mealPreset = mealPresetRepository.findById(presetId)
                .orElseThrow(() -> new EntityNotFoundException("Repas pré-enregistré introuvable avec l'ID: " + presetId));

        if (!mealPreset.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à modifier ce repas");
        }

        mealPreset.setName(request.getName());

        // Suppression explicite des anciens ingrédients dans la table
        mealIngredientRepository.deleteByMealPresetId(mealPreset.getId());
        mealPreset.getIngredients().clear();
        mealPresetRepository.saveAndFlush(mealPreset);

        // Ajout des nouveaux
        List<MealIngredient> newIngredients = request.getIngredients().stream().map(ingReq -> {
            FoodItem foodItem = foodItemRepository.findById(ingReq.getFoodItemId())
                    .orElseThrow(() -> new EntityNotFoundException("Aliment introuvable avec l'ID: " + ingReq.getFoodItemId()));
            return MealIngredient.builder()
                    .mealPreset(mealPreset)
                    .foodItem(foodItem)
                    .quantityGrams(ingReq.getQuantityGrams())
                    .build();
        }).collect(Collectors.toList());

        mealPreset.getIngredients().addAll(newIngredients);
        MealPreset savedPreset = mealPresetRepository.save(mealPreset);

        return mapToResponseWithTotals(savedPreset);
    }

    @Transactional(readOnly = true)
    public List<MealPresetResponse> getUserMealPresets(Long userId) {
        log.info("Récupération de tous les repas pré-enregistrés de l'utilisateur ID: {}", userId);
        
        List<MealPreset> presets = mealPresetRepository.findAllByUserId(userId);
        
        return presets.stream()
                .map(this::mapToResponseWithTotals)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MealPresetResponse getMealPresetById(Long presetId, Long userId) {
        MealPreset preset = mealPresetRepository.findById(presetId)
                .orElseThrow(() -> new EntityNotFoundException("Repas pré-enregistré introuvable avec l'ID: " + presetId));
                
        if (!preset.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à voir ce repas");
        }
        
        return mapToResponseWithTotals(preset);
    }

    @Transactional
    public void deleteMealPreset(Long presetId, Long userId) {
        log.info("Suppression du repas pré-enregistré ID: {} par l'utilisateur ID: {}", presetId, userId);
        
        MealPreset preset = mealPresetRepository.findById(presetId)
                .orElseThrow(() -> new EntityNotFoundException("Repas pré-enregistré introuvable avec l'ID: " + presetId));
                
        if (!preset.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à supprimer ce repas");
        }

        // Détacher / supprimer les références avant suppression pour éviter la violation FK.
        // L'historique est détaché (totaux déjà agrégés sur daily_log) ; le planning est purgé.
        logEntryRepository.detachMealPreset(presetId);
        plannedMealEntryRepository.deleteByMealPresetId(presetId);

        mealPresetRepository.delete(preset);
    }

    // Calcul manuel sûr et certain des totaux pour éviter les problèmes de MapStruct
    private MealPresetResponse mapToResponseWithTotals(MealPreset preset) {
        MealPresetResponse response = mealPresetMapper.toResponse(preset);
        totalsCalculator.applyTotals(preset, response);
        return response;
    }
}