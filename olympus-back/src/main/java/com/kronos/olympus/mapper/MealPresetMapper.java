package com.kronos.olympus.mapper;

import com.kronos.olympus.dto.response.MealIngredientResponse;
import com.kronos.olympus.dto.response.MealPresetResponse;
import com.kronos.olympus.model.MealIngredient;
import com.kronos.olympus.model.MealPreset;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = {FoodItemMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MealPresetMapper {

    MealPresetResponse toResponse(MealPreset mealPreset);

    MealIngredientResponse toIngredientResponse(MealIngredient ingredient);

    // Le calcul des totaux a été déplacé dans MealPresetService.mapToResponseWithTotals
    // pour garantir que les données de la base soient toujours fraiches lors du calcul.
}
