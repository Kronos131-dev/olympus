-- Planification des repas : repas par défaut appliqués automatiquement selon une récurrence

-- Drapeau anti-réapplication + verrou optimiste sur le journal quotidien
ALTER TABLE daily_logs ADD COLUMN IF NOT EXISTS plan_applied BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE daily_logs ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Marque les entrées générées automatiquement depuis un plan
ALTER TABLE log_entries ADD COLUMN IF NOT EXISTS from_plan BOOLEAN NOT NULL DEFAULT false;

CREATE TABLE IF NOT EXISTS meal_plans (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT NOT NULL REFERENCES users(id),
    name                 VARCHAR(255) NOT NULL,
    recurrence_type      VARCHAR(30) NOT NULL,
    weekdays_mask        VARCHAR(120),
    anchor_date          DATE,
    custom_interval_days INTEGER,
    start_date           DATE,
    end_date             DATE,
    active               BOOLEAN NOT NULL DEFAULT true,
    created_at           TIMESTAMP NOT NULL,
    updated_at           TIMESTAMP NOT NULL,
    CONSTRAINT meal_plans_recurrence_type_check
        CHECK (recurrence_type IN ('DAILY', 'SPECIFIC_WEEKDAYS', 'EVERY_OTHER_DAY', 'CUSTOM'))
);

CREATE TABLE IF NOT EXISTS planned_meal_entries (
    id              BIGSERIAL PRIMARY KEY,
    meal_plan_id    BIGINT NOT NULL REFERENCES meal_plans(id),
    food_item_id    BIGINT REFERENCES food_items(id),
    meal_preset_id  BIGINT REFERENCES meal_presets(id),
    quantity_grams  DOUBLE PRECISION,
    planned_time    TIME
);

CREATE INDEX IF NOT EXISTS idx_meal_plans_user_id ON meal_plans (user_id);
CREATE INDEX IF NOT EXISTS idx_planned_meal_entries_meal_plan_id ON planned_meal_entries (meal_plan_id);
