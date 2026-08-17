ALTER TABLE food_items ADD COLUMN IF NOT EXISTS ciqual_code INTEGER;

CREATE UNIQUE INDEX IF NOT EXISTS uk_food_items_ciqual_code
    ON food_items (ciqual_code) WHERE ciqual_code IS NOT NULL;

CREATE TABLE IF NOT EXISTS food_item_nutrients (
    food_item_id    BIGINT NOT NULL REFERENCES food_items(id) ON DELETE CASCADE,
    nutrient        VARCHAR(40) NOT NULL,
    amount_per_100g DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (food_item_id, nutrient)
);

