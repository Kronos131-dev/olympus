package com.kronos.olympus.service.mealplan;

import com.kronos.olympus.dto.request.PlannedMealEntryRequest;
import com.kronos.olympus.dto.request.WeeklyPlanRequest;
import com.kronos.olympus.dto.response.MealPlanResponse;
import com.kronos.olympus.dto.response.MealPresetResponse;
import com.kronos.olympus.dto.response.PlannedMealEntryResponse;
import com.kronos.olympus.mapper.MealPlanMapper;
import com.kronos.olympus.model.FoodItem;
import com.kronos.olympus.model.MealIngredient;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Vérifie que le planning hebdomadaire renvoie les vrais totaux nutritionnels des repas (Fix 2).
 * Le mapper MapStruct ne calcule pas les totaux d'un {@link MealPreset} : sans le recalcul, le
 * planning affichait 0 kcal. On utilise le vrai {@link MealPresetTotalsCalculator} pour valider
 * le bout-en-bout du calcul appliqué sur la réponse mappée.
 */
@ExtendWith(MockitoExtension.class)
class MealPlanServiceTest {

    @Mock
    private MealPlanRepository mealPlanRepository;
    @Mock
    private PlannedMealEntryRepository plannedMealEntryRepository;
    @Mock
    private FoodItemRepository foodItemRepository;
    @Mock
    private MealPresetRepository mealPresetRepository;
    @Mock
    private MealPlanMapper mealPlanMapper;

    private MealPlanService mealPlanService;
    private User user;
    private MealPreset preset;

    @BeforeEach
    void setUp() {
        // Vrai calculateur (logique pure) pour tester le calcul réel, mapper mocké.
        mealPlanService = new MealPlanService(
                mealPlanRepository, plannedMealEntryRepository, foodItemRepository,
                mealPresetRepository, mealPlanMapper, new MealPresetTotalsCalculator());

        user = User.builder().id(1L).build();

        // 200 g d'un aliment à 100 kcal/100 g → 200 kcal, 20 P, 40 G, 10 L.
        FoodItem food = FoodItem.builder()
                .id(3L).name("Riz").kcal100g(100.0).proteins100g(10.0).carbs100g(20.0).fats100g(5.0)
                .build();
        MealIngredient ingredient = MealIngredient.builder()
                .foodItem(food).quantityGrams(200.0).build();
        preset = MealPreset.builder()
                .id(7L).user(user).name("Bol").ingredients(List.of(ingredient)).build();
    }

    /** Réponse mappée « brute » telle que la produirait MapStruct : preset sans totaux (0/null). */
    private MealPlanResponse mappedResponseWithPreset() {
        PlannedMealEntryResponse entryResp = PlannedMealEntryResponse.builder()
                .mealPreset(MealPresetResponse.builder().id(7L).name("Bol").build())
                .build();
        return MealPlanResponse.builder().entries(new ArrayList<>(List.of(entryResp))).build();
    }

    private MealPlan weeklyPlanWithPreset() {
        PlannedMealEntry entry = PlannedMealEntry.builder()
                .mealPreset(preset).quantityGrams(1.0).dayOfWeek(DayOfWeek.MONDAY).build();
        return MealPlan.builder()
                .id(50L).user(user).recurrenceType(RecurrenceType.WEEKLY).active(true)
                .plannedEntries(new ArrayList<>(List.of(entry))).build();
    }

    @Test
    void getWeeklyPlan_appliqueLesVraisTotauxAuxRepasPlanifies() {
        MealPlan plan = weeklyPlanWithPreset();
        when(mealPlanRepository.findFirstByUserIdAndRecurrenceType(1L, RecurrenceType.WEEKLY))
                .thenReturn(Optional.of(plan));
        when(mealPlanMapper.toResponse(plan)).thenReturn(mappedResponseWithPreset());

        MealPlanResponse result = mealPlanService.getWeeklyPlan(user);

        MealPresetResponse mp = result.getEntries().get(0).getMealPreset();
        assertEquals(200.0, mp.getTotalKcal());
        assertEquals(20.0, mp.getTotalProteins());
        assertEquals(40.0, mp.getTotalCarbs());
        assertEquals(10.0, mp.getTotalFats());
    }

    @Test
    void saveWeeklyPlan_appliqueLesVraisTotauxDansLaReponse() {
        MealPlan plan = MealPlan.builder()
                .id(50L).user(user).recurrenceType(RecurrenceType.WEEKLY).active(true)
                .plannedEntries(new ArrayList<>()).build();
        when(mealPlanRepository.findFirstByUserIdAndRecurrenceType(1L, RecurrenceType.WEEKLY))
                .thenReturn(Optional.of(plan));
        when(mealPresetRepository.findById(7L)).thenReturn(Optional.of(preset));
        when(mealPlanRepository.save(any(MealPlan.class))).thenAnswer(i -> i.getArgument(0));
        when(mealPlanMapper.toResponse(any(MealPlan.class))).thenReturn(mappedResponseWithPreset());

        WeeklyPlanRequest request = WeeklyPlanRequest.builder()
                .entries(List.of(PlannedMealEntryRequest.builder()
                        .mealPresetId(7L).dayOfWeek(DayOfWeek.MONDAY).build()))
                .build();

        MealPlanResponse result = mealPlanService.saveWeeklyPlan(user, request);

        assertEquals(200.0, result.getEntries().get(0).getMealPreset().getTotalKcal());
    }
}
