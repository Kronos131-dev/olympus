package com.kronos.olympus.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "log_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_log_id", nullable = false)
    private DailyLog dailyLog;

    // Peut être null si on logge un repas entier
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_item_id")
    private FoodItem foodItem;

    // Peut être null si on logge un aliment unitaire
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_preset_id")
    private MealPreset mealPreset;

    // Si c'est un MealPreset, le quantityGrams peut être 1.0 (ou null, selon la règle choisie), 
    // mais pour un aliment unitaire ce sera la quantité en grammes (ex: 150g).
    @Column(nullable = true)
    private Double quantityGrams;

    private String unit;
    private Double amount;

    @Column(nullable = false)
    private LocalDateTime consumedAt;

    // True si l'entrée provient de la matérialisation automatique d'un plan de repas
    @Builder.Default
    @Column(nullable = false)
    private Boolean fromPlan = false;

    @PrePersist
    protected void onCreate() {
        if (this.consumedAt == null) {
            this.consumedAt = LocalDateTime.now();
        }
        if (this.fromPlan == null) {
            this.fromPlan = false;
        }
    }
}
