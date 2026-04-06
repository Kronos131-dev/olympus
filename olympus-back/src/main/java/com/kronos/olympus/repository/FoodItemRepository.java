package com.kronos.olympus.repository;

import com.kronos.olympus.model.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.kronos.olympus.model.enums.FoodSource;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {

    // Pour vérifier si l'aliment est déjà dans notre base locale (Fallback cache) via le scan
    Optional<FoodItem> findByBarcode(String barcode);
    boolean existsBySource(FoodSource source);
    // Recherche d'aliments par nom (insensible à la casse) pour la recherche textuelle
    List<FoodItem> findByNameContainingIgnoreCase(String name);
    List<FoodItem> findByNameContainingIgnoreCaseAndSource(String name, FoodSource source);
}
