package com.kronos.olympus.mapper;

import com.kronos.olympus.dto.response.MealIngredientResponse;
import com.kronos.olympus.dto.response.MealPresetResponse;
import com.kronos.olympus.model.MealIngredient;
import com.kronos.olympus.model.MealPreset;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", uses = {FoodItemMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MealPresetMapper {

    MealPresetResponse toResponse(MealPreset mealPreset);

    MealIngredientResponse toIngredientResponse(MealIngredient ingredient);

    // MapStruct appellera cette méthode après le mapping standard
    // Cela nous permet de calculer à la volée les totaux de macros pour ce repas, sans le stocker en base
    @AfterMapping
    default void calculateTotals(MealPreset mealPreset, @MappingTarget MealPresetResponse response) {
        if (mealPreset.getIngredients() == null || mealPreset.getIngredients().isEmpty()) {
            response.setTotalKcal(0.0);
            response.setTotalProteins(0.0);
            response.setTotalCarbs(0.0);
            response.setTotalFats(0.0);
            return;
        }

        double totalKcal = 0;
        double totalProteins = 0;
        double totalCarbs = 0;
        double totalFats = 0;

        for (MealIngredient ingredient : mealPreset.getIngredients()) {
            if (ingredient.getFoodItem() != null && ingredient.getQuantityGrams() != null) {
                double ratio = ingredient.getQuantityGrams() / 100.0;
                totalKcal += ingredient.getFoodItem().getKcal100g() * ratio;
                totalProteins += ingredient.getFoodItem().getProteins100g() * ratio;
                totalCarbs += ingredient.getFoodItem().getCarbs100g() * ratio;
                totalFats += ingredient.getFoodItem().getFats100g() * ratio;
            }
        }

        response.setTotalKcal(Math.round(totalKcal * 100.0) / 100.0);
        response.setTotalProteins(Math.round(totalProteins * 100.0) / 100.0);
        response.setTotalCarbs(Math.round(totalCarbs * 100.0) / 100.0);
        response.setTotalFats(Math.round(totalFats * 100.0) / 100.0);
    }
}
