package com.kronos.olympus.repository;

import com.kronos.olympus.model.UserMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserMetricsRepository extends JpaRepository<UserMetrics, Long> {

    // Pour obtenir tout l'historique de poids de l'utilisateur
    List<UserMetrics> findAllByUserIdOrderByRecordedDateAsc(Long userId);

    // Pour des graphiques sur une période donnée
    List<UserMetrics> findAllByUserIdAndRecordedDateBetweenOrderByRecordedDateAsc(Long userId, LocalDate startDate, LocalDate endDate);

    // Récupérer la dernière métrique enregistrée (le poids actuel)
    Optional<UserMetrics> findTopByUserIdOrderByRecordedDateDesc(Long userId);
}
