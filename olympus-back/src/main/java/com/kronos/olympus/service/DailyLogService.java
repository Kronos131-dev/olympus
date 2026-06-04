package com.kronos.olympus.service;

import com.kronos.olympus.dto.request.LogEntryRequest;
import com.kronos.olympus.dto.request.UpdateActivityRequest;
import com.kronos.olympus.dto.response.DailyLogResponse;
import com.kronos.olympus.exception.EntityNotFoundException;
import com.kronos.olympus.mapper.DailyLogMapper;
import com.kronos.olympus.model.*;
import com.kronos.olympus.model.enums.ActivityLevel;
import com.kronos.olympus.repository.DailyLogRepository;
import com.kronos.olympus.repository.FoodItemRepository;
import com.kronos.olympus.repository.LogEntryRepository;
import com.kronos.olympus.repository.MealPlanRepository;
import com.kronos.olympus.repository.MealPresetRepository;
import com.kronos.olympus.service.mealplan.RecurrenceEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyLogService {

    private final DailyLogRepository dailyLogRepository;
    private final LogEntryRepository logEntryRepository;
    private final FoodItemRepository foodItemRepository;
    private final MealPresetRepository mealPresetRepository;
    private final MealPlanRepository mealPlanRepository;
    private final RecurrenceEvaluator recurrenceEvaluator;
    private final DailyLogMapper dailyLogMapper;
    private final MealPresetTotalsCalculator mealPresetTotalsCalculator;

    @Transactional
    public DailyLogResponse getDailyLogByDate(User user, LocalDate date) {
        DailyLog dailyLog = getOrCreateDailyLog(user, date);
        // Matérialisation automatique du plan de repas si la journée n'a pas encore été remplie
        applyPlanIfEligible(user, dailyLog);
        return toResponseWithTotals(dailyLog);
    }

    @Transactional
    public DailyLogResponse updateActivity(User user, UpdateActivityRequest request) {
        DailyLog dailyLog = getOrCreateDailyLog(user, request.getTargetDate());

        if (request.getStepCount() != null) {
            dailyLog.setStepCount(request.getStepCount());
        }

        if (request.getWorkoutDurationMinutes() != null) {
            dailyLog.setWorkoutDurationMinutes(request.getWorkoutDurationMinutes());
        }

        if (request.getManualKcalBurned() != null) {
            dailyLog.setManualKcalBurned(request.getManualKcalBurned());
        }

        // Définir les pas minimums requis par rapport au niveau d'activité de l'utilisateur
        int minSteps = getMinStepsForActivityLevel(user.getActivityLevel());

        // On calcule les calories brûlées (ou perdues) par rapport au palier
        int stepDifference = dailyLog.getStepCount() - minSteps;
        double kcalDifferenceFromSteps = (stepDifference / 1000.0) * 40.0;

        // Entraînement : 1 minute = ~6 kcal
        double burnedKcalWorkout = dailyLog.getWorkoutDurationMinutes() * 6.0;

        // Total
        double totalExtraKcal = kcalDifferenceFromSteps + burnedKcalWorkout + dailyLog.getManualKcalBurned();

        dailyLog.setExtraKcalBurned(round(totalExtraKcal));

        dailyLogRepository.save(dailyLog);
        return toResponseWithTotals(dailyLog);
    }

    @Transactional
    public DailyLogResponse addLogEntry(User user, LogEntryRequest request) {
        log.info("Ajout d'une entrée au journal pour l'utilisateur {} à la date {}", user.getId(), request.getTargetDate());

        if (request.getFoodItemId() == null && request.getMealPresetId() == null) {
            throw new IllegalArgumentException("Vous devez fournir soit un ID d'aliment, soit un ID de repas pré-enregistré");
        }

        DailyLog dailyLog = getOrCreateDailyLog(user, request.getTargetDate());

        FoodItem foodItem = null;
        MealPreset mealPreset = null;

        if (request.getFoodItemId() != null) {
            foodItem = foodItemRepository.findById(request.getFoodItemId())
                    .orElseThrow(() -> new EntityNotFoundException("Aliment introuvable avec l'ID: " + request.getFoodItemId()));
        } else {
            mealPreset = mealPresetRepository.findById(request.getMealPresetId())
                    .orElseThrow(() -> new EntityNotFoundException("Repas pré-enregistré introuvable avec l'ID: " + request.getMealPresetId()));
            if (!mealPreset.getUser().getId().equals(user.getId())) {
                throw new IllegalArgumentException("Vous n'êtes pas autorisé à utiliser ce repas");
            }
        }

        LogEntry logEntry = buildLogEntry(dailyLog, foodItem, mealPreset, request.getQuantityGrams());

        // Une journée commencée manuellement ne doit plus être écrasée par le plan de repas
        if (Boolean.FALSE.equals(dailyLog.getPlanApplied())) {
            dailyLog.setPlanApplied(true);
        }

        logEntryRepository.save(logEntry);

        // Ajouter l'entrée à la collection en mémoire (comme applyPlanIfEligible) : sinon la
        // réponse mappée depuis dailyLog.getEntries() omet la nouvelle entrée et elle
        // n'apparaît pas dans l'historique côté front.
        if (dailyLog.getEntries() == null) {
            dailyLog.setEntries(new ArrayList<>());
        }
        dailyLog.getEntries().add(logEntry);

        dailyLogRepository.save(dailyLog); // Met à jour les totaux en base

        return toResponseWithTotals(dailyLog);
    }

    @Transactional
    public DailyLogResponse removeLogEntry(User user, Long entryId) {
        log.info("Suppression de l'entrée ID: {} par l'utilisateur ID: {}", entryId, user.getId());

        LogEntry logEntry = logEntryRepository.findById(entryId)
                .orElseThrow(() -> new EntityNotFoundException("Entrée introuvable avec l'ID: " + entryId));

        DailyLog dailyLog = logEntry.getDailyLog();

        if (!dailyLog.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à modifier ce journal");
        }

        double removedKcal = 0;
        double removedProteins = 0;
        double removedCarbs = 0;
        double removedFats = 0;

        if (logEntry.getFoodItem() != null) {
            double ratio = logEntry.getQuantityGrams() / 100.0;
            removedKcal = logEntry.getFoodItem().getKcal100g() * ratio;
            removedProteins = logEntry.getFoodItem().getProteins100g() * ratio;
            removedCarbs = logEntry.getFoodItem().getCarbs100g() * ratio;
            removedFats = logEntry.getFoodItem().getFats100g() * ratio;

        } else if (logEntry.getMealPreset() != null) {
            for (MealIngredient ingredient : logEntry.getMealPreset().getIngredients()) {
                double ratio = ingredient.getQuantityGrams() / 100.0;
                removedKcal += ingredient.getFoodItem().getKcal100g() * ratio;
                removedProteins += ingredient.getFoodItem().getProteins100g() * ratio;
                removedCarbs += ingredient.getFoodItem().getCarbs100g() * ratio;
                removedFats += ingredient.getFoodItem().getFats100g() * ratio;
            }
        }

        // On soustrait et on s'assure de ne pas avoir de valeurs négatives à cause des arrondis
        dailyLog.setTotalKcal(Math.max(0, round(dailyLog.getTotalKcal() - removedKcal)));
        dailyLog.setTotalProteins(Math.max(0, round(dailyLog.getTotalProteins() - removedProteins)));
        dailyLog.setTotalCarbs(Math.max(0, round(dailyLog.getTotalCarbs() - removedCarbs)));
        dailyLog.setTotalFats(Math.max(0, round(dailyLog.getTotalFats() - removedFats)));

        // Suppression de l'entrée et mise à jour du log.
        // NB : on ne touche jamais à planApplied — supprimer toutes les entrées ne fait pas revenir le plan.
        if (dailyLog.getEntries() != null) {
            dailyLog.getEntries().remove(logEntry);
        }
        logEntryRepository.delete(logEntry);
        dailyLogRepository.save(dailyLog);

        return toResponseWithTotals(dailyLog);
    }

    /**
     * Matérialise les repas planifiés dans la journée si elle est encore vierge.
     * Ne fait rien si la journée a déjà été initialisée (planApplied) ou contient déjà des entrées.
     */
    private void applyPlanIfEligible(User user, DailyLog dailyLog) {
        if (Boolean.TRUE.equals(dailyLog.getPlanApplied())) {
            return;
        }
        if (dailyLog.getEntries() != null && !dailyLog.getEntries().isEmpty()) {
            return;
        }

        List<MealPlan> activePlans = mealPlanRepository.findAllByUserIdAndActiveTrue(user.getId());
        if (activePlans.isEmpty()) {
            return;
        }

        if (dailyLog.getEntries() == null) {
            dailyLog.setEntries(new ArrayList<>());
        }

        boolean anyApplied = false;
        for (MealPlan plan : activePlans) {
            if (!recurrenceEvaluator.matches(plan, dailyLog.getTargetDate())) {
                continue;
            }
            if (plan.getPlannedEntries() == null) {
                continue;
            }
            for (PlannedMealEntry planned : plan.getPlannedEntries()) {
                // Plan hebdomadaire : seules les entrées du jour courant s'appliquent
                if (planned.getDayOfWeek() != null
                        && planned.getDayOfWeek() != dailyLog.getTargetDate().getDayOfWeek()) {
                    continue;
                }
                LogEntry entry = buildLogEntry(dailyLog, planned.getFoodItem(), planned.getMealPreset(), planned.getQuantityGrams());
                entry.setFromPlan(true);
                entry.setConsumedAt(planned.getPlannedTime() != null
                        ? dailyLog.getTargetDate().atTime(planned.getPlannedTime())
                        : dailyLog.getTargetDate().atTime(12, 0));
                logEntryRepository.save(entry);
                dailyLog.getEntries().add(entry);
                anyApplied = true;
            }
        }

        // On ne marque la journée comme initialisée que si le plan a réellement produit des entrées :
        // une journée sans plan correspondant reste ré-évaluable si un plan est créé plus tard.
        if (anyApplied) {
            dailyLog.setPlanApplied(true);
            dailyLogRepository.save(dailyLog);
        }
    }

    /**
     * Mappe le journal puis renseigne les totaux des entrées « repas » : MapStruct ne calcule
     * pas les totaux d'un {@link MealPreset}, ce qui affichait 0 kcal/macros au front. On les
     * recalcule ici à partir des entités, par id de preset.
     */
    private DailyLogResponse toResponseWithTotals(DailyLog dailyLog) {
        DailyLogResponse response = dailyLogMapper.toResponse(dailyLog);
        if (response.getEntries() == null || dailyLog.getEntries() == null) {
            return response;
        }
        Map<Long, MealPreset> presetsById = dailyLog.getEntries().stream()
                .map(LogEntry::getMealPreset)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toMap(MealPreset::getId, p -> p, (a, b) -> a));

        response.getEntries().forEach(entry -> {
            if (entry.getMealPreset() != null) {
                MealPreset preset = presetsById.get(entry.getMealPreset().getId());
                if (preset != null) {
                    mealPresetTotalsCalculator.applyTotals(preset, entry.getMealPreset());
                }
            }
        });
        return response;
    }

    /**
     * Construit une {@link LogEntry} pour un aliment OU un repas pré-enregistré et applique
     * ses macros aux totaux du {@link DailyLog}. Logique partagée entre l'ajout manuel et la
     * matérialisation d'un plan.
     */
    private LogEntry buildLogEntry(DailyLog dailyLog, FoodItem foodItem, MealPreset mealPreset, Double quantityGrams) {
        LogEntry logEntry = LogEntry.builder()
                .dailyLog(dailyLog)
                .consumedAt(LocalDateTime.now())
                .build();

        double addedKcal = 0;
        double addedProteins = 0;
        double addedCarbs = 0;
        double addedFats = 0;

        if (foodItem != null) {
            if (quantityGrams == null || quantityGrams <= 0) {
                throw new IllegalArgumentException("La quantité est obligatoire et doit être positive pour un aliment unitaire");
            }
            logEntry.setFoodItem(foodItem);
            logEntry.setQuantityGrams(quantityGrams);

            double ratio = quantityGrams / 100.0;
            addedKcal = foodItem.getKcal100g() * ratio;
            addedProteins = foodItem.getProteins100g() * ratio;
            addedCarbs = foodItem.getCarbs100g() * ratio;
            addedFats = foodItem.getFats100g() * ratio;

        } else if (mealPreset != null) {
            logEntry.setMealPreset(mealPreset);
            logEntry.setQuantityGrams(1.0); // 1 portion du repas

            for (MealIngredient ingredient : mealPreset.getIngredients()) {
                double ratio = ingredient.getQuantityGrams() / 100.0;
                addedKcal += ingredient.getFoodItem().getKcal100g() * ratio;
                addedProteins += ingredient.getFoodItem().getProteins100g() * ratio;
                addedCarbs += ingredient.getFoodItem().getCarbs100g() * ratio;
                addedFats += ingredient.getFoodItem().getFats100g() * ratio;
            }
        } else {
            throw new IllegalArgumentException("Vous devez fournir soit un aliment, soit un repas pré-enregistré");
        }

        dailyLog.setTotalKcal(round(dailyLog.getTotalKcal() + addedKcal));
        dailyLog.setTotalProteins(round(dailyLog.getTotalProteins() + addedProteins));
        dailyLog.setTotalCarbs(round(dailyLog.getTotalCarbs() + addedCarbs));
        dailyLog.setTotalFats(round(dailyLog.getTotalFats() + addedFats));

        return logEntry;
    }

    private DailyLog getOrCreateDailyLog(User user, LocalDate date) {
        return dailyLogRepository.findByUserIdAndTargetDate(user.getId(), date)
                .orElseGet(() -> {
                    int minSteps = getMinStepsForActivityLevel(user.getActivityLevel());
                    return dailyLogRepository.save(
                        DailyLog.builder()
                                .user(user)
                                .targetDate(date)
                                .totalKcal(0.0)
                                .totalProteins(0.0)
                                .totalCarbs(0.0)
                                .totalFats(0.0)
                                .stepCount(minSteps) // Initialise avec le minimum requis
                                .workoutDurationMinutes(0)
                                .manualKcalBurned(0)
                                .extraKcalBurned(0.0) // Commence à 0 car on est au minimum
                                .build()
                    );
                });
    }

    private int getMinStepsForActivityLevel(ActivityLevel level) {
        if (level == null) return 8000; // Default
        return switch (level) {
            case SEDENTARY -> 3000;
            case LIGHT -> 5000;
            case MODERATE -> 8000;
            case INTENSE -> 10000;
        };
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
