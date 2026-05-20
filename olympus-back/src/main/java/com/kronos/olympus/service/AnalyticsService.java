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
        
        double totalCalorieDeficit = 0; // Somme de (Calories dépensées - Calories mangées)

        // 3. Boucler sur chaque jour de la période pour construire les points du graphique
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        
        // Pour gérer les jours où l'utilisateur n'a pas de métriques enregistrées, on garde en mémoire la dernière métrique connue
        UserMetrics lastKnownMetric = null;
        if (!metrics.isEmpty()) {
            lastKnownMetric = metrics.get(0); // Approximation
        }
        
        for (int i = 0; i <= daysBetween; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            
            DailyLog logForDay = logsByDate.get(currentDate);
            UserMetrics metricForDay = metricsByDate.get(currentDate);
            
            if (metricForDay != null) {
                lastKnownMetric = metricForDay;
            }
            
            Double weight = null;
            if (metricForDay != null && metricForDay.getWeightKg() != null) {
                weight = metricForDay.getWeightKg();
                totalWeightSum += weight;
                daysWithWeight++;
            }
            
            Double kcal = null;
            Double proteins = null;
            Double carbs = null;
            Double fats = null;
            Double extraKcalBurned = 0.0;
            
            if (logForDay != null) {
                if (logForDay.getTotalKcal() > 0) {
                    kcal = logForDay.getTotalKcal();
                    proteins = logForDay.getTotalProteins();
                    carbs = logForDay.getTotalCarbs();
                    fats = logForDay.getTotalFats();
                    
                    totalKcalSum += kcal;
                    daysWithLogs++;
                }
                extraKcalBurned = logForDay.getExtraKcalBurned() != null ? logForDay.getExtraKcalBurned() : 0.0;
            }

            Double targetKcalForDay = null;
            if (lastKnownMetric != null && lastKnownMetric.getCalorieGoal() != null) {
                targetKcalForDay = lastKnownMetric.getCalorieGoal().doubleValue();
                
                // Si on a les deux (target et mangé), on peut calculer le déficit de ce jour
                if (kcal != null) {
                    // Déficit = Ce que j'ai le droit de manger (Base + Sport) - Ce que j'ai mangé
                    double dailyDeficit = (targetKcalForDay + extraKcalBurned) - kcal;
                    totalCalorieDeficit += dailyDeficit;
                }
            }

            dailyData.add(AnalyticsResponse.DailyMetricPoint.builder()
                    .date(currentDate)
                    .weightKg(weight)
                    .targetKcal(targetKcalForDay != null ? targetKcalForDay + extraKcalBurned : null) // La ligne cible est = TDEE + Sport
                    .totalKcal(kcal)
                    .totalProteins(proteins)
                    .totalCarbs(carbs)
                    .totalFats(fats)
                    .extraKcalBurned(extraKcalBurned)
                    .build());
        }

        // 4. Calculer les moyennes et l'estimation de graisse perdue
        Double averageKcal = daysWithLogs > 0 ? round(totalKcalSum / daysWithLogs) : null;
        Double averageWeight = daysWithWeight > 0 ? round(totalWeightSum / daysWithWeight) : null;
        
        // 1 gramme de graisse corporelle = environ 7.7 kcal
        Double estimatedFatLossGrams = 0.0;
        if (totalCalorieDeficit > 0) {
            estimatedFatLossGrams = round(totalCalorieDeficit / 7.7);
        } else if (totalCalorieDeficit < 0) {
            // S'il est en surplus (prise de masse ou trop mangé), on l'indique en négatif ou 0. On peut l'appeler weight gained
            estimatedFatLossGrams = round(totalCalorieDeficit / 7.7); // C'est une estimation brute
        }

        return AnalyticsResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .averageKcal(averageKcal)
                .averageWeight(averageWeight)
                .estimatedFatLossGrams(estimatedFatLossGrams)
                .dailyData(dailyData)
                .build();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
