package com.kronos.olympus.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "meal_presets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = "ingredients")
public class MealPreset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    // Permet de charger la liste des ingrédients d'un preset (orphanRemoval permet de les supprimer en cascade)
    @OneToMany(mappedBy = "mealPreset", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MealIngredient> ingredients;
}
