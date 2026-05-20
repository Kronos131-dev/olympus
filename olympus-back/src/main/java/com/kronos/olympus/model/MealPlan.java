package com.kronos.olympus.model;

import com.kronos.olympus.model.enums.RecurrenceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Modèle de planification : des repas par défaut appliqués automatiquement
 * selon une récurrence quand une journée n'a pas encore été remplie.
 */
@Entity
@Table(name = "meal_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = "plannedEntries")
@ToString(exclude = "plannedEntries")
public class MealPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RecurrenceType recurrenceType;

    // Pour SPECIFIC_WEEKDAYS : liste de DayOfWeek séparés par des virgules (ex: "MONDAY,WEDNESDAY,FRIDAY")
    @Column(length = 120)
    private String weekdaysMask;

    // Date de référence pour EVERY_OTHER_DAY et CUSTOM (calcul de parité / intervalle)
    @Column
    private LocalDate anchorDate;

    // Pour CUSTOM : intervalle en jours
    @Column
    private Integer customIntervalDays;

    // Fenêtre de validité du plan (optionnelle)
    @Column
    private LocalDate startDate;

    @Column
    private LocalDate endDate;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "mealPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlannedMealEntry> plannedEntries;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.active == null) {
            this.active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
