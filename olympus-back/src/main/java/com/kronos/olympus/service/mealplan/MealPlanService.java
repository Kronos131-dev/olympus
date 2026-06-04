package com.kronos.olympus.service.mealplan;

import com.kronos.olympus.dto.request.MealPlanRequest;
import com.kronos.olympus.dto.request.PlannedMealEntryRequest;
import com.kronos.olympus.dto.request.WeeklyPlanRequest;
import com.kronos.olympus.dto.response.MealPlanResponse;
import com.kronos.olympus.exception.EntityNotFoundException;
import com.kronos.olympus.mapper.MealPlanMapper;
import com.kronos.olympus.model.FoodItem;
import com.kronos.olympus.model.MealPlan;
import com.kronos.olympus.model.MealPreset;
import com.kronos.olympus.model.PlannedMealEntry;
import com.kronos.olympus.model.User;
import com.kronos.olympus.model.enums.RecurrenceType;
import com.kronos.olympus.repository.FoodItemRepository;
import com.kronos.olympus.repository.MealPlanRepository;
import com.kronos.olympus.repository.MealPresetRepository;
import com.kronos.olympus.repository.PlannedMealEntryRepository;
import com.kronos.olympus.service.MealPresetTotalsCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MealPlanService {

    private final MealPlanRepository mealPlanRepository;
    private final PlannedMealEntryRepository plannedMealEntryRepository;
    private final FoodItemRepository foodItemRepository;
    private final MealPresetRepository mealPresetRepository;
    private final MealPlanMapper mealPlanMapper;
    private final MealPresetTotalsCalculator mealPresetTotalsCalculator;

    @Transactional
    public MealPlanResponse createMealPlan(User user, MealPlanRequest request) {
        log.info("Création d'un plan de repas '{}' pour l'utilisateur ID: {}", request.getName(), user.getId());
        validateRecurrence(request);

        MealPlan plan = MealPlan.builder()
                .user(user)
                .name(request.getName())
                .recurrenceType(request.getRecurrenceType())
                .weekdaysMask(request.getWeekdaysMask())
                .anchorDate(request.getAnchorDate() != null ? request.getAnchorDate() : LocalDate.now())
                .customIntervalDays(request.getCustomIntervalDays())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .active(request.getActive() == null ? Boolean.TRUE : request.getActive())
                .build();

        plan.setPlannedEntries(buildPlannedEntries(user, plan, request.getEntries()));

        return mealPlanMapper.toResponse(mealPlanRepository.save(plan));
    }

    @Transactional
    public MealPlanResponse updateMealPlan(User user, Long planId, MealPlanRequest request) {
        log.info("Mise à jour du plan de repas ID: {} pour l'utilisateur ID: {}", planId, user.getId());
        validateRecurrence(request);

        MealPlan plan = mealPlanRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("Plan de repas introuvable avec l'ID: " + planId));

        if (!plan.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à modifier ce plan");
        }

        plan.setName(request.getName());
        plan.setRecurrenceType(request.getRecurrenceType());
        plan.setWeekdaysMask(request.getWeekdaysMask());
        plan.setAnchorDate(request.getAnchorDate() != null ? request.getAnchorDate() : LocalDate.now());
        plan.setCustomIntervalDays(request.getCustomIntervalDays());
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
        if (request.getActive() != null) {
            plan.setActive(request.getActive());
        }

        // Remplacement explicite des éléments planifiés (même logique que MealPresetService)
        plannedMealEntryRepository.deleteByMealPlanId(plan.getId());
        if (plan.getPlannedEntries() != null) {
            plan.getPlannedEntries().clear();
        } else {
            plan.setPlannedEntries(new ArrayList<>());
        }
        mealPlanRepository.saveAndFlush(plan);

        plan.getPlannedEntries().addAll(buildPlannedEntries(user, plan, request.getEntries()));

        return mealPlanMapper.toResponse(mealPlanRepository.save(plan));
    }

    @Transactional(readOnly = true)
    public List<MealPlanResponse> getUserMealPlans(Long userId) {
        return mealPlanRepository.findAllByUserId(userId).stream()
                .map(mealPlanMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MealPlanResponse getMealPlanById(Long planId, Long userId) {
        MealPlan plan = mealPlanRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("Plan de repas introuvable avec l'ID: " + planId));

        if (!plan.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à voir ce plan");
        }

        return mealPlanMapper.toResponse(plan);
    }

    @Transactional
    public void deleteMealPlan(Long planId, Long userId) {
        MealPlan plan = mealPlanRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("Plan de repas introuvable avec l'ID: " + planId));

        if (!plan.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à supprimer ce plan");
        }

        mealPlanRepository.delete(plan);
    }

    /** Récupère l'emploi du temps hebdomadaire de l'utilisateur (le crée vide s'il n'existe pas). */
    @Transactional
    public MealPlanResponse getWeeklyPlan(User user) {
        return toResponseWithTotals(getOrCreateWeeklyPlanEntity(user));
    }

    /** Enregistre l'emploi du temps hebdomadaire : remplace toutes les entrées des 7 jours. */
    @Transactional
    public MealPlanResponse saveWeeklyPlan(User user, WeeklyPlanRequest request) {
        MealPlan plan = getOrCreateWeeklyPlanEntity(user);

        List<PlannedMealEntryRequest> entries = request.getEntries() != null
                ? request.getEntries() : new ArrayList<>();
        for (PlannedMealEntryRequest entry : entries) {
            if (entry.getDayOfWeek() == null) {
                throw new IllegalArgumentException("Chaque repas planifié doit indiquer un jour de la semaine");
            }
        }

        // Remplacement des entrées via orphanRemoval. On évite un DELETE bulk : il entrerait
        // en conflit avec les entrées déjà chargées (EntityGraph) et provoquerait une
        // StaleStateException au flush dès le 2e enregistrement.
        List<PlannedMealEntry> newEntries = buildPlannedEntries(user, plan, entries);
        if (plan.getPlannedEntries() == null) {
            plan.setPlannedEntries(new ArrayList<>());
        }
        plan.getPlannedEntries().clear();
        plan.getPlannedEntries().addAll(newEntries);

        return toResponseWithTotals(mealPlanRepository.save(plan));
    }

    /**
     * Mappe le plan puis renseigne les totaux des entrées « repas » : MapStruct ne calcule pas
     * les totaux d'un {@link MealPreset}, ce qui affichait 0 kcal/macros au planning. On les
     * recalcule ici à partir des entités, par id de preset (miroir de DailyLogService).
     */
    private MealPlanResponse toResponseWithTotals(MealPlan plan) {
        MealPlanResponse response = mealPlanMapper.toResponse(plan);
        if (response.getEntries() == null || plan.getPlannedEntries() == null) {
            return response;
        }
        Map<Long, MealPreset> presetsById = plan.getPlannedEntries().stream()
                .map(PlannedMealEntry::getMealPreset)
                .filter(Objects::nonNull)
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

    private MealPlan getOrCreateWeeklyPlanEntity(User user) {
        return mealPlanRepository
                .findFirstByUserIdAndRecurrenceType(user.getId(), RecurrenceType.WEEKLY)
                .orElseGet(() -> mealPlanRepository.save(MealPlan.builder()
                        .user(user)
                        .name("Ma semaine")
                        .recurrenceType(RecurrenceType.WEEKLY)
                        .anchorDate(LocalDate.now())
                        .active(true)
                        .plannedEntries(new ArrayList<>())
                        .build()));
    }

    private void validateRecurrence(MealPlanRequest request) {
        if (request.getRecurrenceType() == null) {
            throw new IllegalArgumentException("Le type de récurrence est obligatoire");
        }
        switch (request.getRecurrenceType()) {
            case SPECIFIC_WEEKDAYS:
                if (request.getWeekdaysMask() == null || request.getWeekdaysMask().isBlank()) {
                    throw new IllegalArgumentException("Au moins un jour de la semaine doit être sélectionné");
                }
                break;
            case CUSTOM:
                if (request.getCustomIntervalDays() == null || request.getCustomIntervalDays() <= 0) {
                    throw new IllegalArgumentException("L'intervalle personnalisé (en jours) doit être un entier positif");
                }
                break;
            default:
                break;
        }
    }

    private List<PlannedMealEntry> buildPlannedEntries(User user, MealPlan plan, List<PlannedMealEntryRequest> requests) {
        List<PlannedMealEntry> entries = new ArrayList<>();
        for (PlannedMealEntryRequest req : requests) {
            boolean hasFood = req.getFoodItemId() != null;
            boolean hasPreset = req.getMealPresetId() != null;
            if (hasFood == hasPreset) {
                throw new IllegalArgumentException(
                        "Chaque élément planifié doit référencer soit un aliment, soit un repas pré-enregistré (et un seul)");
            }

            PlannedMealEntry.PlannedMealEntryBuilder builder = PlannedMealEntry.builder()
                    .mealPlan(plan)
                    .dayOfWeek(req.getDayOfWeek())
                    .plannedTime(req.getPlannedTime());

            if (hasFood) {
                FoodItem foodItem = foodItemRepository.findById(req.getFoodItemId())
                        .orElseThrow(() -> new EntityNotFoundException("Aliment introuvable avec l'ID: " + req.getFoodItemId()));
                if (req.getQuantityGrams() == null || req.getQuantityGrams() <= 0) {
                    throw new IllegalArgumentException("La quantité est obligatoire et positive pour un aliment planifié");
                }
                builder.foodItem(foodItem).quantityGrams(req.getQuantityGrams());
            } else {
                MealPreset mealPreset = mealPresetRepository.findById(req.getMealPresetId())
                        .orElseThrow(() -> new EntityNotFoundException("Repas pré-enregistré introuvable avec l'ID: " + req.getMealPresetId()));
                if (!mealPreset.getUser().getId().equals(user.getId())) {
                    throw new IllegalArgumentException("Vous n'êtes pas autorisé à utiliser ce repas");
                }
                builder.mealPreset(mealPreset).quantityGrams(1.0); // 1 portion du repas
            }

            entries.add(builder.build());
        }
        return entries;
    }
}
