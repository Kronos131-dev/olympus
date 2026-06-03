package com.kronos.olympus.repository;

import com.kronos.olympus.model.MealPlan;
import com.kronos.olympus.model.enums.RecurrenceType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {

    // EntityGraph volontairement peu profond (foodItem / mealPreset uniquement) :
    // les ingrédients d'un mealPreset sont chargés en lazy dans la transaction
    // pour éviter une MultipleBagFetchException (deux collections List jointes).
    @EntityGraph(attributePaths = {"plannedEntries", "plannedEntries.foodItem", "plannedEntries.mealPreset"})
    List<MealPlan> findAllByUserId(Long userId);

    @EntityGraph(attributePaths = {"plannedEntries", "plannedEntries.foodItem", "plannedEntries.mealPreset"})
    List<MealPlan> findAllByUserIdAndActiveTrue(Long userId);

    @EntityGraph(attributePaths = {"plannedEntries", "plannedEntries.foodItem", "plannedEntries.mealPreset"})
    Optional<MealPlan> findFirstByUserIdAndRecurrenceType(Long userId, RecurrenceType recurrenceType);
}
