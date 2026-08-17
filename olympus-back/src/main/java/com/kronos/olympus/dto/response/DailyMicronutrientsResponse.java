package com.kronos.olympus.dto.response;

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
public class DailyMicronutrientsResponse {
    private LocalDate targetDate;

    @Builder.Default
    private List<MicronutrientResponse> nutrients = List.of();

    private Double overallCoverage;
}
