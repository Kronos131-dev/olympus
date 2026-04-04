package com.kronos.olympus.repository;

import com.kronos.olympus.model.DailyLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyLogRepository extends JpaRepository<DailyLog, Long> {

    // Pour charger la journée de l'utilisateur avec ses entrées
    @EntityGraph(attributePaths = {"entries", "entries.foodItem", "entries.mealPreset"})
    Optional<DailyLog> findByUserIdAndTargetDate(Long userId, LocalDate targetDate);

    // Requête spécifique pour ramener uniquement les logs sans le détail (pour les graphiques/analytics)
    @Query("SELECT d FROM DailyLog d WHERE d.user.id = :userId AND d.targetDate BETWEEN :startDate AND :endDate ORDER BY d.targetDate ASC")
    List<DailyLog> findLogsForAnalytics(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
