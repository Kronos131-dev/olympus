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
import com.kronos.olympus.repository.MealPresetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MealPresetService {

    private final MealPresetRepository mealPresetRepository;
    private final FoodItemRepository foodItemRepository;
    private final MealPresetMapper mealPresetMapper;

    @Transactional
    public MealPresetResponse createMealPreset(User user, MealPresetRequest request) {
        log.info("Création d'un nouveau repas pré-enregistré '{}' pour l'utilisateur ID: {}", request.getName(), user.getId());

        // Création de l'entité mère (le repas)
        MealPreset mealPreset = MealPreset.builder()
                .user(user)
                .name(request.getName())
                .build();

        // Création de la liste des ingrédients
        List<MealIngredient> ingredients = request.getIngredients().stream().map(ingReq -> {
            // Vérification que l'aliment existe bien dans notre base locale (cache)
            FoodItem foodItem = foodItemRepository.findById(ingReq.getFoodItemId())
                    .orElseThrow(() -> new EntityNotFoundException("Aliment introuvable avec l'ID: " + ingReq.getFoodItemId() + ". Veuillez le scanner d'abord."));
            
            return MealIngredient.builder()
                    .mealPreset(mealPreset) // Lien bidirectionnel
                    .foodItem(foodItem)
                    .quantityGrams(ingReq.getQuantityGrams())
                    .build();
        }).collect(Collectors.toList());

        // Ajout des ingrédients au repas
        mealPreset.setIngredients(ingredients);
        
        // Sauvegarde en cascade (grâce à CascadeType.ALL sur la relation dans MealPreset)
        MealPreset savedPreset = mealPresetRepository.save(mealPreset);
        
        return mealPresetMapper.toResponse(savedPreset);
    }

    @Transactional(readOnly = true)
    public List<MealPresetResponse> getUserMealPresets(Long userId) {
        log.info("Récupération de tous les repas pré-enregistrés de l'utilisateur ID: {}", userId);
        
        // La méthode findAllByUserId utilise un @EntityGraph pour charger les ingrédients efficacement en une seule requête SQL
        List<MealPreset> presets = mealPresetRepository.findAllByUserId(userId);
        
        return presets.stream()
                .map(mealPresetMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MealPresetResponse getMealPresetById(Long presetId, Long userId) {
        MealPreset preset = mealPresetRepository.findById(presetId)
                .orElseThrow(() -> new EntityNotFoundException("Repas pré-enregistré introuvable avec l'ID: " + presetId));
                
        // Vérification de sécurité pour s'assurer qu'un utilisateur n'accède pas au repas d'un autre
        if (!preset.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à voir ce repas");
        }
        
        return mealPresetMapper.toResponse(preset);
    }

    @Transactional
    public void deleteMealPreset(Long presetId, Long userId) {
        log.info("Suppression du repas pré-enregistré ID: {} par l'utilisateur ID: {}", presetId, userId);
        
        MealPreset preset = mealPresetRepository.findById(presetId)
                .orElseThrow(() -> new EntityNotFoundException("Repas pré-enregistré introuvable avec l'ID: " + presetId));
                
        // Vérification de sécurité
        if (!preset.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à supprimer ce repas");
        }
        
        // Grâce au orphanRemoval = true dans l'entité, la suppression du preset supprimera automatiquement les MealIngredients liés
        mealPresetRepository.delete(preset);
    }
}
