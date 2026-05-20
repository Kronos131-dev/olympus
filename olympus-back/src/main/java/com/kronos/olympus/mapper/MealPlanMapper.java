package com.kronos.olympus.mapper;

import com.kronos.olympus.dto.response.MealPlanResponse;
import com.kronos.olympus.dto.response.PlannedMealEntryResponse;
import com.kronos.olympus.model.MealPlan;
import com.kronos.olympus.model.PlannedMealEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = {FoodItemMapper.class, MealPresetMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MealPlanMapper {

    @Mapping(source = "plannedEntries", target = "entries")
    MealPlanResponse toResponse(MealPlan mealPlan);

    PlannedMealEntryResponse toEntryResponse(PlannedMealEntry entry);
}
