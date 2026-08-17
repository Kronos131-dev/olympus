-- Les quatre totaux du palier A sont déclarés NOT NULL sur l'entité DailyLog. Hibernate
-- (ddl-auto: update) les ajoute sans DEFAULT, ce que PostgreSQL refuse dès que la table
-- contient des lignes : « column contains null values ». L'ALTER échoue, Hibernate se
-- contente d'un WARN, et l'application tourne ensuite avec des colonnes manquantes.
--
-- On les crée donc ici, avec une valeur par défaut, avant que Hibernate ne tente sa propre
-- migration : les journées existantes repartent de 0 et sont de toute façon recalculées
-- depuis leurs entrées au premier accès (DailyLogService.recalculateTotals).
ALTER TABLE daily_logs ADD COLUMN IF NOT EXISTS total_fibers        DOUBLE PRECISION NOT NULL DEFAULT 0;
ALTER TABLE daily_logs ADD COLUMN IF NOT EXISTS total_sugars        DOUBLE PRECISION NOT NULL DEFAULT 0;
ALTER TABLE daily_logs ADD COLUMN IF NOT EXISTS total_saturated_fat DOUBLE PRECISION NOT NULL DEFAULT 0;
ALTER TABLE daily_logs ADD COLUMN IF NOT EXISTS total_salt          DOUBLE PRECISION NOT NULL DEFAULT 0;
