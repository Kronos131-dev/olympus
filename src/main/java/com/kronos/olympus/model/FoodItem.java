package com.kronos.olympus.model;

import com.kronos.olympus.model.enums.FoodSource;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "food_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String barcode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double kcal100g;

    @Column(nullable = false)
    private Double proteins100g;

    @Column(nullable = false)
    private Double carbs100g;

    @Column(nullable = false)
    private Double fats100g;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FoodSource source;
}
