package com.kronos.olympus.model.enums;

/**
 * Type de récurrence d'un plan de repas.
 */
public enum RecurrenceType {
    /** Tous les jours. */
    DAILY,
    /** Certains jours de la semaine (voir weekdaysMask). */
    SPECIFIC_WEEKDAYS,
    /** Un jour sur deux à partir de la date d'ancrage. */
    EVERY_OTHER_DAY,
    /** Tous les N jours à partir de la date d'ancrage (voir customIntervalDays). */
    CUSTOM,
    /** Emploi du temps hebdomadaire : chaque entrée porte son jour (voir PlannedMealEntry.dayOfWeek). */
    WEEKLY
}
