package com.kronos.olympus.mapper;

import com.kronos.olympus.dto.response.DailyLogResponse;
import com.kronos.olympus.dto.response.LogEntryResponse;
import com.kronos.olympus.model.DailyLog;
import com.kronos.olympus.model.LogEntry;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = {FoodItemMapper.class, MealPresetMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DailyLogMapper {

    DailyLogResponse toResponse(DailyLog dailyLog);
    
    LogEntryResponse toEntryResponse(LogEntry entry);
}
