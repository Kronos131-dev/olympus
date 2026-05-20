package com.kronos.olympus.service.mealplan;

import com.kronos.olympus.model.MealPlan;
import com.kronos.olympus.model.enums.RecurrenceType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Test unitaire pur (sans Spring) de l'évaluation des récurrences de plans de repas. */
class RecurrenceEvaluatorTest {

    private final RecurrenceEvaluator evaluator = new RecurrenceEvaluator();

    // 2024-01-01 est un lundi
    private static final LocalDate MONDAY = LocalDate.of(2024, 1, 1);
    private static final LocalDate TUESDAY = LocalDate.of(2024, 1, 2);
    private static final LocalDate FRIDAY = LocalDate.of(2024, 1, 5);

    @Test
    void daily_matchesAnyDate() {
        MealPlan plan = MealPlan.builder()
                .recurrenceType(RecurrenceType.DAILY)
                .active(true)
                .build();
        assertTrue(evaluator.matches(plan, MONDAY));
        assertTrue(evaluator.matches(plan, FRIDAY));
    }

    @Test
    void specificWeekdays_matchesOnlyListedDays() {
        MealPlan plan = MealPlan.builder()
                .recurrenceType(RecurrenceType.SPECIFIC_WEEKDAYS)
                .weekdaysMask("MONDAY,FRIDAY")
                .active(true)
                .build();
        assertTrue(evaluator.matches(plan, MONDAY));
        assertTrue(evaluator.matches(plan, FRIDAY));
        assertFalse(evaluator.matches(plan, TUESDAY));
    }

    @Test
    void everyOtherDay_matchesParityFromAnchor() {
        MealPlan plan = MealPlan.builder()
                .recurrenceType(RecurrenceType.EVERY_OTHER_DAY)
                .anchorDate(MONDAY)
                .active(true)
                .build();
        assertTrue(evaluator.matches(plan, MONDAY));
        assertTrue(evaluator.matches(plan, MONDAY.plusDays(2)));
        assertFalse(evaluator.matches(plan, MONDAY.plusDays(1)));
    }

    @Test
    void custom_matchesEveryNDaysFromAnchor() {
        MealPlan plan = MealPlan.builder()
                .recurrenceType(RecurrenceType.CUSTOM)
                .anchorDate(MONDAY)
                .customIntervalDays(3)
                .active(true)
                .build();
        assertTrue(evaluator.matches(plan, MONDAY));
        assertTrue(evaluator.matches(plan, MONDAY.plusDays(3)));
        assertFalse(evaluator.matches(plan, MONDAY.plusDays(1)));
    }

    @Test
    void validityWindow_excludesDatesOutsideRange() {
        MealPlan plan = MealPlan.builder()
                .recurrenceType(RecurrenceType.DAILY)
                .startDate(LocalDate.of(2024, 1, 10))
                .endDate(LocalDate.of(2024, 1, 20))
                .active(true)
                .build();
        assertTrue(evaluator.matches(plan, LocalDate.of(2024, 1, 15)));
        assertFalse(evaluator.matches(plan, LocalDate.of(2024, 1, 5)));
        assertFalse(evaluator.matches(plan, LocalDate.of(2024, 1, 25)));
    }

    @Test
    void inactivePlan_neverMatches() {
        MealPlan plan = MealPlan.builder()
                .recurrenceType(RecurrenceType.DAILY)
                .active(false)
                .build();
        assertFalse(evaluator.matches(plan, MONDAY));
    }
}
