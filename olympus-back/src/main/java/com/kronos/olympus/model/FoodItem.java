package com.kronos.olympus.model;

import com.kronos.olympus.model.enums.FoodSource;
import com.kronos.olympus.model.enums.Nutrient;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.LinkedHashMap;
import java.util.Map;

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

    @Column(unique = true)
    private Integer ciqualCode;

    @Column(nullable = false)
    private String name;

    private String foodGroup;

    private String foodSubGroup;

    @Column(nullable = false)
    private Double kcal100g;

    @Column(nullable = false)
    private Double proteins100g;

    @Column(nullable = false)
    private Double carbs100g;

    @Column(nullable = false)
    private Double fats100g;

    private Double fibers100g;

    private Double sugars100g;

    private Double saturatedFat100g;

    private Double salt100g;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "food_item_nutrients", joinColumns = @JoinColumn(name = "food_item_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "nutrient")
    @Column(name = "amount_per_100g", nullable = false)
    @BatchSize(size = 50)
    @Builder.Default
    private Map<Nutrient, Double> micros100g = new LinkedHashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FoodSource source;
}
