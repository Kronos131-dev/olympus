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
public class DailyLogResponse {
    private Long id;
    private LocalDate targetDate;
    private Double totalKcal;
    private Double totalProteins;
    private Double totalCarbs;
    private Double totalFats;
    
    // Nouveaux champs pour tracker l'activité
    private Integer stepCount;
    private Integer workoutDurationMinutes;
    private Integer manualKcalBurned;
    private Double extraKcalBurned;

    // True si la journée a déjà été initialisée (par un plan ou manuellement)
    private Boolean planApplied;

    private List<LogEntryResponse> entries;
}