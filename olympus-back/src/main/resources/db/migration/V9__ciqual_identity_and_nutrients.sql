-- Identité stable des aliments CIQUAL : l'alim_code de l'ANSES permet de rejouer l'import
-- d'un ciqual.csv enrichi sans créer de doublon, là où l'ancien garde-fou (« il y a déjà des
-- lignes CIQUAL, on ne fait rien ») rendait toute mise à jour de la table impossible.
ALTER TABLE food_items ADD COLUMN IF NOT EXISTS ciqual_code INTEGER;

CREATE UNIQUE INDEX IF NOT EXISTS uk_food_items_ciqual_code
    ON food_items (ciqual_code) WHERE ciqual_code IS NOT NULL;

-- Micronutriments pour 100 g. L'absence de ligne signifie « valeur non déterminée par l'ANSES
-- pour cet aliment » : c'est cette distinction avec le zéro qui permet d'afficher un taux de
-- couverture plutôt que des carences imaginaires.
CREATE TABLE IF NOT EXISTS food_item_nutrients (
    food_item_id    BIGINT NOT NULL REFERENCES food_items(id) ON DELETE CASCADE,
    nutrient        VARCHAR(40) NOT NULL,
    amount_per_100g DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (food_item_id, nutrient)
);

-- app_metadata (version du référentiel CIQUAL déjà chargée) n'est pas créée ici : c'est une
-- entité, donc Hibernate la bâtit aussi bien en ddl-auto update qu'en create-drop, alors que
-- le profil de test désactive Flyway.

-- Pas d'index trigramme sur le nom : searchSmartCiqual filtre sur unaccent(name), et unaccent()
-- est STABLE et non IMMUTABLE, donc inutilisable dans une expression d'index sans fonction
-- enveloppe. Sur quelques milliers de lignes, le parcours séquentiel est de toute façon immédiat.
