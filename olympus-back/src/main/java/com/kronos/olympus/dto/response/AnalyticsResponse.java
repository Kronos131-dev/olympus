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
    
    // Total de graisse estimée perdue sur la période
    private Double estimatedFatLossGrams;
    
    // Détail journalier (Points pour tracer le graphique)
    private List<DailyMetricPoint> dailyData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyMetricPoint {
        private LocalDate date;
        private Double weightKg;      // Peut être nul s'il ne s'est pas pesé ce jour-là
        
        // Quotas journaliers de l'utilisateur ce jour-là (pour tracer la courbe limite)
        private Double targetKcal;
        
        private Double totalKcal;     // Peut être nul s'il n'a rien loggé
        private Double totalProteins;
        private Double totalCarbs;
        private Double totalFats;
        
        // Nouveau: pour récupérer le total dépensé dans la journée (et l'afficher dans les stats si besoin)
        private Double extraKcalBurned;
    }
}