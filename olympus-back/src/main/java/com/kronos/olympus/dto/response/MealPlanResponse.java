package com.kronos.olympus.dto.response;

import com.kronos.olympus.model.enums.RecurrenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanResponse {
    private Long id;
    private String name;
    private RecurrenceType recurrenceType;
    private String weekdaysMask;
    private LocalDate anchorDate;
    private Integer customIntervalDays;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean active;
    private List<PlannedMealEntryResponse> entries;
}
