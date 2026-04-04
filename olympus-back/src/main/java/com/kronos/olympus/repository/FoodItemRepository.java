package com.kronos.olympus.repository;

import com.kronos.olympus.model.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {

    // Pour vérifier si l'aliment est déjà dans notre base locale (Fallback cache) via le scan
    Optional<FoodItem> findByBarcode(String barcode);

    // Recherche d'aliments par nom (insensible à la casse) pour la recherche textuelle
    List<FoodItem> findByNameContainingIgnoreCase(String name);
}
