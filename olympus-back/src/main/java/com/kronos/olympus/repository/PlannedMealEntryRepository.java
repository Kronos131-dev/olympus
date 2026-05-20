package com.kronos.olympus.repository;

import com.kronos.olympus.model.PlannedMealEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlannedMealEntryRepository extends JpaRepository<PlannedMealEntry, Long> {

    @Modifying
    @Query("DELETE FROM PlannedMealEntry p WHERE p.mealPlan.id = :mealPlanId")
    void deleteByMealPlanId(@Param("mealPlanId") Long mealPlanId);
}
