package com.kronos.olympus.repository;

import com.kronos.olympus.model.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {
    // Les opérations de base (sauvegarde, suppression) seront héritées de JpaRepository.

    // Détache un repas pré-enregistré de l'historique avant sa suppression.
    // Les totaux sont déjà agrégés sur daily_log → préservés.
    @Modifying
    @Query("UPDATE LogEntry e SET e.mealPreset = null WHERE e.mealPreset.id = :id")
    void detachMealPreset(@Param("id") Long id);
}
