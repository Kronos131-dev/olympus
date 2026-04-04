package com.kronos.olympus.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meal_ingredients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_preset_id", nullable = false)
    private MealPreset mealPreset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_item_id", nullable = false)
    private FoodItem foodItem;

    @Column(nullable = false)
    private Double quantityGrams;
}
