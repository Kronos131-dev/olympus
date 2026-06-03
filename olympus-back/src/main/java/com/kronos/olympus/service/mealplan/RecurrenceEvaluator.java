package com.kronos.olympus.service.mealplan;

import com.kronos.olympus.model.MealPlan;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Détermine si un {@link MealPlan} s'applique à une date donnée selon sa règle de récurrence.
 */
@Component
public class RecurrenceEvaluator {

    public boolean matches(MealPlan plan, LocalDate date) {
        if (plan == null || plan.getRecurrenceType() == null) {
            return false;
        }
        if (Boolean.FALSE.equals(plan.getActive())) {
            return false;
        }
        // Fenêtre de validité
        if (plan.getStartDate() != null && date.isBefore(plan.getStartDate())) {
            return false;
        }
        if (plan.getEndDate() != null && date.isAfter(plan.getEndDate())) {
            return false;
        }

        switch (plan.getRecurrenceType()) {
            case DAILY:
            case WEEKLY:
                // WEEKLY : le filtrage par jour se fait au niveau de chaque entrée (dayOfWeek)
                return true;

            case SPECIFIC_WEEKDAYS:
                if (plan.getWeekdaysMask() == null || plan.getWeekdaysMask().isBlank()) {
                    return false;
                }
                String today = date.getDayOfWeek().name(); // MONDAY, TUESDAY, ...
                for (String token : plan.getWeekdaysMask().split(",")) {
                    String day = token.trim().toUpperCase();
                    if (!day.isEmpty() && today.startsWith(day)) {
                        return true;
                    }
                }
                return false;

            case EVERY_OTHER_DAY:
                if (plan.getAnchorDate() == null) {
                    return false;
                }
                return Math.floorMod(ChronoUnit.DAYS.between(plan.getAnchorDate(), date), 2) == 0;

            case CUSTOM:
                if (plan.getAnchorDate() == null
                        || plan.getCustomIntervalDays() == null
                        || plan.getCustomIntervalDays() <= 0) {
                    return false;
                }
                return Math.floorMod(
                        ChronoUnit.DAYS.between(plan.getAnchorDate(), date),
                        plan.getCustomIntervalDays()) == 0;

            default:
                return false;
        }
    }
}
