package com.kronos.olympus.service;

import com.kronos.olympus.dto.request.LogEntryRequest;
import com.kronos.olympus.dto.request.UpdateActivityRequest;
import com.kronos.olympus.dto.request.UpdateLogEntryRequest;
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
        // Recalcul depuis les entrées : auto-corrige les journées déjà désynchronisées
        // (totaux fantômes hérités d'un ancien bug ou d'un repas pré-enregistré supprimé).
        recalculateTotals(dailyLog);
        dailyLogRepository.save(dailyLog);
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

        LogEntry logEntry = buildLogEntry(dailyLog, foodItem, mealPreset, request.getQuantityGrams(),
                request.getUnit(), request.getAmount());

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

        recalculateTotals(dailyLog);
        dailyLogRepository.save(dailyLog); // Persiste les totaux recalculés

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

        // Suppression de l'entrée puis recalcul des totaux depuis les entrées restantes.
        // NB : on ne touche jamais à planApplied — supprimer toutes les entrées ne fait pas revenir le plan.
        if (dailyLog.getEntries() != null) {
            dailyLog.getEntries().remove(logEntry);
        }
        logEntryRepository.delete(logEntry);

        recalculateTotals(dailyLog);
        dailyLogRepository.save(dailyLog);

        return toResponseWithTotals(dailyLog);
    }

    @Transactional
    public DailyLogResponse updateLogEntry(User user, Long entryId, UpdateLogEntryRequest request) {
        log.info("Mise à jour de la quantité de l'entrée ID: {} par l'utilisateur ID: {}", entryId, user.getId());

        LogEntry logEntry = logEntryRepository.findById(entryId)
                .orElseThrow(() -> new EntityNotFoundException("Entrée introuvable avec l'ID: " + entryId));

        DailyLog dailyLog = logEntry.getDailyLog();

        if (!dailyLog.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à modifier ce journal");
        }
        if (logEntry.getMealPreset() != null) {
            throw new IllegalArgumentException("La quantité d'un repas pré-enregistré n'est pas modifiable (toujours 1 portion)");
        }

        logEntry.setQuantityGrams(request.getQuantityGrams());
        logEntry.setUnit(request.getUnit());
        logEntry.setAmount(request.getAmount());
        logEntryRepository.save(logEntry);

        recalculateTotals(dailyLog);
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
                LogEntry entry = buildLogEntry(dailyLog, planned.getFoodItem(), planned.getMealPreset(),
                        planned.getQuantityGrams(), null, null);
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
            recalculateTotals(dailyLog);
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
     * Construit une {@link LogEntry} pour un aliment OU un repas pré-enregistré. Les totaux du
     * jour ne sont PAS mutés ici : ils sont recalculés depuis les entrées via
     * {@link #recalculateTotals(DailyLog)} (source de vérité unique, anti-désynchronisation).
     */
    private LogEntry buildLogEntry(DailyLog dailyLog, FoodItem foodItem, MealPreset mealPreset, Double quantityGrams,
            String unit, Double amount) {
        LogEntry logEntry = LogEntry.builder()
                .dailyLog(dailyLog)
                .consumedAt(LocalDateTime.now())
                .build();

        if (foodItem != null) {
            if (quantityGrams == null || quantityGrams <= 0) {
                throw new IllegalArgumentException("La quantité est obligatoire et doit être positive pour un aliment unitaire");
            }
            logEntry.setFoodItem(foodItem);
            logEntry.setQuantityGrams(quantityGrams);
            logEntry.setUnit(unit);
            logEntry.setAmount(amount);
        } else if (mealPreset != null) {
            logEntry.setMealPreset(mealPreset);
            logEntry.setQuantityGrams(1.0); // 1 portion du repas
        } else {
            throw new IllegalArgumentException("Vous devez fournir soit un aliment, soit un repas pré-enregistré");
        }

        return logEntry;
    }

    /**
     * Recalcule les totaux macros du jour à partir des entrées (source de vérité). Évite toute
     * désynchronisation : entrée orpheline (repas pré-enregistré supprimé → mealPreset détaché),
     * édition d'ingrédients après log, double application de plan, etc. Une entrée sans aliment
     * ni repas contribue 0. N'affecte pas l'activité (extraKcalBurned, pas…).
     */
    private void recalculateTotals(DailyLog dailyLog) {
        NutrientTotals totals = new NutrientTotals();
        if (dailyLog.getEntries() != null) {
            for (LogEntry entry : dailyLog.getEntries()) {
                if (entry.getFoodItem() != null && entry.getQuantityGrams() != null) {
                    totals.add(entry.getFoodItem(), entry.getQuantityGrams());
                } else if (entry.getMealPreset() != null && entry.getMealPreset().getIngredients() != null) {
                    for (MealIngredient ing : entry.getMealPreset().getIngredients()) {
                        if (ing.getFoodItem() != null && ing.getQuantityGrams() != null) {
                            totals.add(ing.getFoodItem(), ing.getQuantityGrams());
                        }
                    }
                }
            }
        }
        totals.applyTo(dailyLog);
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

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
