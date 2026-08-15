-- Tables « cœur » créées historiquement par Hibernate (ddl-auto: update) et jamais
-- par une migration. Problème : Flyway s'exécute AVANT Hibernate, donc sur une base
-- neuve les migrations suivantes (V2 ALTER food_items, V4 ALTER daily_logs/log_entries,
-- V3/V4/V5/V7/V8 REFERENCES users, ...) échouaient car ces tables n'existaient pas encore.
--
-- On les crée ici en amont (version 1.1, juste après les extensions et avant V2), en
-- ne posant que le strict nécessaire pour débloquer les migrations : la clé primaire
-- (cible des clés étrangères) et les colonnes référencées par une contrainte ultérieure
-- (food_items.source, requis par le CHECK de V2). Hibernate ddl-auto: update complète
-- ensuite le reste des colonnes à partir des entités, comme il le faisait déjà.
--
-- CREATE TABLE IF NOT EXISTS -> no-op sur les bases existantes (dev) qui ont déjà ces
-- tables ; création réelle sur une base neuve (prod).

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS food_items (
    id     BIGSERIAL PRIMARY KEY,
    -- Référencée par le CHECK de V2 ; mappée depuis l'enum FoodSource (EnumType.STRING).
    source VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS meal_presets (
    id BIGSERIAL PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS daily_logs (
    id BIGSERIAL PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS log_entries (
    id BIGSERIAL PRIMARY KEY
);
