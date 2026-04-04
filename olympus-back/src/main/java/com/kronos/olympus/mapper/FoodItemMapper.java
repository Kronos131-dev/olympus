package com.kronos.olympus.mapper;

import com.kronos.olympus.dto.response.FoodItemResponse;
import com.kronos.olympus.model.FoodItem;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FoodItemMapper {

    FoodItemResponse toResponse(FoodItem foodItem);
}
