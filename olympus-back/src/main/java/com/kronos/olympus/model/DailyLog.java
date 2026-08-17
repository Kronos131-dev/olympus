package com.kronos.olympus.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "daily_logs", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "target_date"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate targetDate;

    // Totaux calculés et stockés pour des requêtes d'analytics rapides
    @Builder.Default
    @Column(nullable = false)
    private Double totalKcal = 0.0;

    @Builder.Default
    @Column(nullable = false)
    private Double totalProteins = 0.0;

    @Builder.Default
    @Column(nullable = false)
    private Double totalCarbs = 0.0;

    @Builder.Default
    @Column(nullable = false)
    private Double totalFats = 0.0;

    @Builder.Default
    @Column(nullable = false)
    private Double totalFibers = 0.0;

    @Builder.Default
    @Column(nullable = false)
    private Double totalSugars = 0.0;

    @Builder.Default
    @Column(nullable = false)
    private Double totalSaturatedFat = 0.0;

    @Builder.Default
    @Column(nullable = false)
    private Double totalSalt = 0.0;

    // Nouveaux champs pour tracker l'activité physique du jour
    @Builder.Default
    @Column(nullable = false)
    private Integer stepCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer workoutDurationMinutes = 0; // Durée de la séance de muscu/sport

    @Builder.Default
    @Column(nullable = false)
    private Integer manualKcalBurned = 0; // Calories brûlées manuellement (cardio, etc.)

    @Builder.Default
    @Column(nullable = false)
    private Double extraKcalBurned = 0.0; // Total des calories brûlées estimées

    // True dès qu'un plan a été matérialisé OU qu'une première entrée a été ajoutée manuellement.
    // Empêche la ré-application du plan (y compris après suppression de toutes les entrées).
    @Builder.Default
    @Column(nullable = false)
    private Boolean planApplied = false;

    // Verrou optimiste : la matérialisation du plan est un effet de bord sur un chemin de lecture
    @Version
    @Builder.Default
    private Long version = 0L;

    @OneToMany(mappedBy = "dailyLog", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LogEntry> entries;
}
