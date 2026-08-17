package com.kronos.olympus.service;

import com.kronos.olympus.dto.response.MealPresetResponse;
import com.kronos.olympus.model.FoodItem;
import com.kronos.olympus.model.MealIngredient;
import com.kronos.olympus.model.MealPreset;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MealPresetTotalsCalculatorTest {
    private final MealPresetTotalsCalculator calculator = new MealPresetTotalsCalculator();

    @Test
    void applyTotals_deuxIngredients_sommeLePalierAYComprisLesFibres() {
        FoodItem rice = FoodItem.builder()
                .name("Riz").kcal100g(130.0).proteins100g(2.7).carbs100g(28.0).fats100g(0.3)
                .fibers100g(1.4).sugars100g(0.1).saturatedFat100g(0.1).salt100g(0.01)
                .build();
        FoodItem lentils = FoodItem.builder()
                .name("Lentilles").kcal100g(116.0).proteins100g(9.0).carbs100g(20.0).fats100g(0.4)
                .fibers100g(7.9).sugars100g(1.8).saturatedFat100g(0.1).salt100g(0.02)
                .build();
        MealPreset preset = MealPreset.builder()
                .name("Riz-lentilles")
                .ingredients(List.of(
                        MealIngredient.builder().foodItem(rice).quantityGrams(200.0).build(),
                        MealIngredient.builder().foodItem(lentils).quantityGrams(100.0).build()))
                .build();

        MealPresetResponse response = new MealPresetResponse();
        calculator.applyTotals(preset, response);

        assertThat(response.getTotalKcal()).isEqualTo(376.0);
        assertThat(response.getTotalProteins()).isEqualTo(14.4);
        assertThat(response.getTotalCarbs()).isEqualTo(76.0);
        assertThat(response.getTotalFats()).isEqualTo(1.0);
        assertThat(response.getTotalFibers()).isEqualTo(10.7);
    }

    @Test
    void applyTotals_ingredientSansFibresConnues_contribueZeroSansEchouer() {
        FoodItem unknownFiber = FoodItem.builder()
                .name("Produit scanné").kcal100g(200.0).proteins100g(10.0).carbs100g(20.0).fats100g(5.0)
                .fibers100g(null)
                .build();
        MealPreset preset = MealPreset.builder()
                .name("Repas partiel")
                .ingredients(List.of(
                        MealIngredient.builder().foodItem(unknownFiber).quantityGrams(100.0).build()))
                .build();

        MealPresetResponse response = new MealPresetResponse();
        calculator.applyTotals(preset, response);

        assertThat(response.getTotalKcal()).isEqualTo(200.0);
        assertThat(response.getTotalFibers()).isEqualTo(0.0);
    }

    @Test
    void applyTotals_presetSansIngredients_renvoieDesTotauxAZero() {
        MealPreset preset = MealPreset.builder().name("Vide").ingredients(List.of()).build();

        MealPresetResponse response = new MealPresetResponse();
        calculator.applyTotals(preset, response);

        assertThat(response.getTotalKcal()).isEqualTo(0.0);
        assertThat(response.getTotalFibers()).isEqualTo(0.0);
        assertThat(response.getTotalSalt()).isEqualTo(0.0);
    }
}
