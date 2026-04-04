package com.kronos.olympus.repository;

import com.kronos.olympus.model.MealPreset;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MealPresetRepository extends JpaRepository<MealPreset, Long> {

    // On utilise un EntityGraph pour éviter le problème N+1 sur le chargement des ingrédients
    // lors de l'affichage de la liste des presets de l'utilisateur.
    @EntityGraph(attributePaths = {"ingredients", "ingredients.foodItem"})
    List<MealPreset> findAllByUserId(Long userId);
}
