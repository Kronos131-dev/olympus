package com.kronos.olympus.service;

import com.kronos.olympus.dto.response.AnalyticsResponse;
import com.kronos.olympus.model.DailyLog;
import com.kronos.olympus.model.UserMetrics;
import com.kronos.olympus.repository.DailyLogRepository;
import com.kronos.olympus.repository.UserMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final DailyLogRepository dailyLogRepository;
    private final UserMetricsRepository userMetricsRepository;

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalyticsForPeriod(Long userId, LocalDate startDate, LocalDate endDate) {
        log.info("Récupération des analytics pour l'utilisateur {} de {} à {}", userId, startDate, endDate);

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("La date de début doit être antérieure à la date de fin");
        }

        // 1. Récupérer les logs quotidiens (seulement les agrégats, grâce à notre requête optimisée)
        List<DailyLog> dailyLogs = dailyLogRepository.findLogsForAnalytics(userId, startDate, endDate);
        Map<LocalDate, DailyLog> logsByDate = dailyLogs.stream()
                .collect(Collectors.toMap(DailyLog::getTargetDate, Function.identity()));

        // 2. Récupérer l'historique de poids sur la même période
        List<UserMetrics> metrics = userMetricsRepository.findAllByUserIdAndRecordedDateBetweenOrderByRecordedDateAsc(userId, startDate, endDate);
        Map<LocalDate, UserMetrics> metricsByDate = metrics.stream()
                .collect(Collectors.toMap(UserMetrics::getRecordedDate, Function.identity(), (existing, replacement) -> existing)); // En cas de doublons le même jour, on garde le premier

        List<AnalyticsResponse.DailyMetricPoint> dailyData = new ArrayList<>();
        double totalKcalSum = 0;
        int daysWithLogs = 0;
        
        double totalWeightSum = 0;
        int daysWithWeight = 0;

        // 3. Boucler sur chaque jour de la période pour construire les points du graphique
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        
        for (int i = 0; i <= daysBetween; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            
            DailyLog logForDay = logsByDate.get(currentDate);
            UserMetrics metricForDay = metricsByDate.get(currentDate);
            
            Double weight = null;
            if (metricForDay != null) {
                weight = metricForDay.getWeightKg();
                totalWeightSum += weight;
                daysWithWeight++;
            }
            
            Double kcal = null;
            Double proteins = null;
            Double carbs = null;
            Double fats = null;
            
            if (logForDay != null && logForDay.getTotalKcal() > 0) {
                kcal = logForDay.getTotalKcal();
                proteins = logForDay.getTotalProteins();
                carbs = logForDay.getTotalCarbs();
                fats = logForDay.getTotalFats();
                
                totalKcalSum += kcal;
                daysWithLogs++;
            }

            dailyData.add(AnalyticsResponse.DailyMetricPoint.builder()
                    .date(currentDate)
                    .weightKg(weight)
                    .totalKcal(kcal)
                    .totalProteins(proteins)
                    .totalCarbs(carbs)
                    .totalFats(fats)
                    .build());
        }

        // 4. Calculer les moyennes
        Double averageKcal = daysWithLogs > 0 ? round(totalKcalSum / daysWithLogs) : null;
        Double averageWeight = daysWithWeight > 0 ? round(totalWeightSum / daysWithWeight) : null;

        return AnalyticsResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .averageKcal(averageKcal)
                .averageWeight(averageWeight)
                .dailyData(dailyData)
                .build();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
