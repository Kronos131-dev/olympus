package com.kronos.olympus.repository;

import com.kronos.olympus.model.MealIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MealIngredientRepository extends JpaRepository<MealIngredient, Long> {
    
    @Modifying
    @Query("DELETE FROM MealIngredient m WHERE m.mealPreset.id = :mealPresetId")
    void deleteByMealPresetId(@Param("mealPresetId") Long mealPresetId);
}
