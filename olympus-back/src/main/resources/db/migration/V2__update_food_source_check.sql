-- On supprime l'ancienne règle
ALTER TABLE food_items DROP CONSTRAINT IF EXISTS food_items_source_check;

-- On recrée la règle avec les valeurs EXACTES de ton enum Java
ALTER TABLE food_items ADD CONSTRAINT food_items_source_check
    CHECK (source IN ('OFF', 'MANUAL', 'CIQUAL', 'AI'));