package com.kronos.olympus.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

/**
 * Un aliment ou un repas pré-enregistré planifié dans un {@link MealPlan}.
 * Calqué sur {@link LogEntry} (foodItem XOR mealPreset) pour une matérialisation directe.
 */
@Entity
@Table(name = "planned_meal_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"mealPlan", "foodItem", "mealPreset"})
@ToString(exclude = {"mealPlan", "foodItem", "mealPreset"})
public class PlannedMealEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_plan_id", nullable = false)
    private MealPlan mealPlan;

    // Null si l'élément planifié est un repas pré-enregistré
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_item_id")
    private FoodItem foodItem;

    // Null si l'élément planifié est un aliment unitaire
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_preset_id")
    private MealPreset mealPreset;

    // Quantité en grammes pour un aliment ; 1.0 (une portion) pour un repas pré-enregistré
    @Column
    private Double quantityGrams;

    // Heure prévue (optionnelle) : utilisée comme consumedAt lors de la matérialisation
    @Column
    private LocalTime plannedTime;
}
