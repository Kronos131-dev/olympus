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
public class AnalyticsResponse {

    private LocalDate startDate;
    private LocalDate endDate;
    
    // Moyennes sur la période
    private Double averageKcal;
    private Double averageWeight;
    
    // Détail journalier (Points pour tracer le graphique)
    private List<DailyMetricPoint> dailyData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyMetricPoint {
        private LocalDate date;
        private Double weightKg;      // Peut être nul s'il ne s'est pas pesé ce jour-là
        private Double totalKcal;     // Peut être nul s'il n'a rien loggé
        private Double totalProteins;
        private Double totalCarbs;
        private Double totalFats;
    }
}
