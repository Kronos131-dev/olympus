-- Emploi du temps hebdomadaire : chaque repas planifié porte son jour de la semaine.

ALTER TABLE planned_meal_entries ADD COLUMN IF NOT EXISTS day_of_week VARCHAR(12);

-- Autorise le nouveau type de récurrence WEEKLY (plan hebdomadaire jour par jour).
ALTER TABLE meal_plans DROP CONSTRAINT IF EXISTS meal_plans_recurrence_type_check;
ALTER TABLE meal_plans ADD CONSTRAINT meal_plans_recurrence_type_check
    CHECK (recurrence_type IN ('DAILY', 'SPECIFIC_WEEKDAYS', 'EVERY_OTHER_DAY', 'CUSTOM', 'WEEKLY'));
