package com.kronos.olympus.service;

import com.kronos.olympus.dto.response.FoodItemDetailResponse;
import com.kronos.olympus.dto.response.FoodItemResponse;
import com.kronos.olympus.exception.EntityNotFoundException;
import com.kronos.olympus.mapper.FoodItemMapper;
import com.kronos.olympus.model.FoodItem;
import com.kronos.olympus.repository.FoodItemRepository;
import com.kronos.olympus.service.integration.OpenFoodFactsClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kronos.olympus.model.enums.FoodSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FoodItemService {

    private final FoodItemRepository foodItemRepository;
    private final OpenFoodFactsClient openFoodFactsClient;
    private final FoodItemMapper foodItemMapper;

    public FoodItemResponse getFoodItemByBarcode(String barcode) {
        log.info("Recherche de l'aliment avec le code-barres : {}", barcode);

        // Étape 1 : Vérifier le cache local (Base de données PostgreSQL)
        Optional<FoodItem> cachedFoodItem = foodItemRepository.findByBarcode(barcode);
        
        if (cachedFoodItem.isPresent()) {
            log.info("Aliment trouvé dans le cache local");
            return foodItemMapper.toResponse(cachedFoodItem.get());
        }

        // Étape 2 : Si non trouvé, interroger l'API externe (Open Food Facts)
        log.info("Aliment non trouvé localement, interrogation de l'API Open Food Facts");
        Optional<FoodItem> externalFoodItem = openFoodFactsClient.fetchProductByBarcode(barcode);

        if (externalFoodItem.isPresent()) {
            // Étape 3 : Sauvegarder dans la base locale (Mise en cache)
            FoodItem savedFoodItem = foodItemRepository.save(externalFoodItem.get());
            log.info("Aliment récupéré via l'API externe et mis en cache : {}", savedFoodItem.getName());
            return foodItemMapper.toResponse(savedFoodItem);
        } else {
            throw new EntityNotFoundException("Aliment introuvable avec le code-barres : " + barcode);
        }
    }

    public List<FoodItemResponse> searchFoodItemsByName(String name) {
        log.info("Recherche textuelle pour l'aliment : {}", name);

        // Recherche d'abord dans la base locale (insensible à la casse, trié par longueur pour avoir les correspondances exactes en premier)
        List<FoodItem> localResults = foodItemRepository.searchByNameOrderedByLength(name)
                .stream()
                .limit(50) // Limite pour ne pas surcharger le front
                .collect(Collectors.toList());
        
        // Si on a des résultats locaux, on les retourne en priorité (on peut aussi choisir de toujours chercher sur OFF)
        if (!localResults.isEmpty()) {
            log.info("{} résultats trouvés localement pour '{}'", localResults.size(), name);
            return localResults.stream()
                    .map(foodItemMapper::toResponse)
                    .collect(Collectors.toList());
        }

        // Si rien en local, on cherche sur Open Food Facts
        log.info("Aucun résultat local pour '{}', recherche sur Open Food Facts", name);
        List<FoodItem> externalResults = openFoodFactsClient.searchProductsByName(name);
        
        if (externalResults.isEmpty()) {
            log.info("Aucun résultat trouvé sur l'API externe pour '{}'", name);
            return List.of(); // Retourne une liste vide au lieu de lever une exception
        }

        // On sauvegarde les résultats de la recherche dans notre base pour les prochaines requêtes
        List<FoodItem> savedResults = foodItemRepository.saveAll(externalResults);
        
        return savedResults.stream()
                .map(foodItemMapper::toResponse)
                .limit(50)
                .collect(Collectors.toList());
    }

    public List<FoodItemResponse> searchCiqualFoods(String query) {
        log.info("Recherche textuelle CIQUAL pour : {}", query);

        if (query == null || query.trim().length() < 2) {
            return List.of(); // On ne cherche rien si on a tapé moins de 2 lettres
        }

        // Recherche intelligente FTS + Trigrammes limités nativement en BDD à 50
        return foodItemRepository.searchSmartCiqual(query.trim(), FoodSource.CIQUAL.name())
                .stream()
                .limit(50)
                .map(foodItemMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FoodItemDetailResponse getFoodItemDetail(Long id) {
        FoodItem food = foodItemRepository.findWithMicrosById(id)
                .orElseThrow(() -> new EntityNotFoundException("Aliment introuvable avec l'ID: " + id));

        return FoodItemDetailResponse.builder()
                .id(food.getId())
                .barcode(food.getBarcode())
                .name(food.getName())
                .source(food.getSource())
                .foodGroup(food.getFoodGroup())
                .foodSubGroup(food.getFoodSubGroup())
                .kcal100g(food.getKcal100g())
                .proteins100g(food.getProteins100g())
                .carbs100g(food.getCarbs100g())
                .fats100g(food.getFats100g())
                .fibers100g(food.getFibers100g())
                .sugars100g(food.getSugars100g())
                .saturatedFat100g(food.getSaturatedFat100g())
                .salt100g(food.getSalt100g())
                .micros100g(new LinkedHashMap<>(food.getMicros100g()))
                .build();
    }
}
