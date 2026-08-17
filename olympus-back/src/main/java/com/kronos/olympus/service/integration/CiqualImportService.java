package com.kronos.olympus.service.integration;

import com.kronos.olympus.model.AppMetadata;
import com.kronos.olympus.model.FoodItem;
import com.kronos.olympus.model.enums.FoodSource;
import com.kronos.olympus.model.enums.Nutrient;
import com.kronos.olympus.repository.AppMetadataRepository;
import com.kronos.olympus.repository.FoodItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Charge la table de composition nutritionnelle de l'ANSES au démarrage.
 *
 * <p>Le fichier est produit par {@code scripts/build-ciqual-csv.py} : aliments bruts uniquement,
 * valeurs pour 100 g, cellule vide quand l'ANSES n'a pas déterminé la valeur.
 *
 * <p>L'import est rejouable. Chaque aliment est retrouvé par son {@code alim_code}, si bien qu'un
 * CSV enrichi met à jour les lignes existantes au lieu d'en créer de nouvelles, et que les
 * {@code LogEntry} de l'historique continuent de pointer vers le bon aliment.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CiqualImportService implements CommandLineRunner {

    private static final String VERSION_KEY = "ciqual.version";
    private static final String RESOURCE = "ciqual.csv";

    // Découpe sur les virgules situées hors guillemets.
    private static final String CSV_SPLIT = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";

    private final FoodItemRepository foodItemRepository;
    private final AppMetadataRepository appMetadataRepository;
    private final JdbcTemplate jdbcTemplate;
    // Transaction pilotée explicitement plutôt que @Transactional : run() doit pouvoir rattraper
    // un échec APRÈS le rollback, ce qu'une méthode annotée ne permet pas depuis l'intérieur.
    private final TransactionTemplate transactionTemplate;

    @Value("${olympus.ciqual.version}")
    private String csvVersion;

    @Override
    public void run(String... args) {
        if (csvVersion.equals(appliedVersion())) {
            log.info("Référentiel CIQUAL déjà en version {}. Import ignoré.", csvVersion);
            return;
        }
        try {
            transactionTemplate.executeWithoutResult(status -> importReferential());
        } catch (Exception e) {
            // Un référentiel qui ne se charge pas ne doit JAMAIS empêcher l'API de démarrer :
            // l'application reste utilisable avec les aliments déjà en base, et l'import sera
            // retenté au prochain démarrage.
            log.error("Import du référentiel CIQUAL échoué : l'application démarre sans mise à jour.", e);
        }
    }

    private void importReferential() {
        log.info("Import du référentiel CIQUAL en version {}...", csvVersion);
        List<Map<String, String>> rows = readCsv();
        if (rows.isEmpty()) {
            log.error("ciqual.csv est vide ou illisible : import abandonné.");
            return;
        }

        Map<Integer, FoodItem> existing = foodItemRepository.findByCiqualCodeIsNotNull().stream()
                .collect(Collectors.toMap(FoodItem::getCiqualCode, item -> item, (a, b) -> a));
        Map<String, FoodItem> legacyByName = legacyCiqualFoodsByName();

        List<FoodItem> toSave = new ArrayList<>();
        int created = 0;
        int adopted = 0;
        for (Map<String, String> row : rows) {
            Integer code = parseCode(row.get("code"));
            String name = row.getOrDefault("nom", "").trim();
            if (code == null || name.isEmpty()) {
                continue;
            }

            FoodItem item = existing.get(code);
            if (item == null) {
                // Bases antérieures à l'alim_code : on adopte la ligne homonyme plutôt que d'en
                // créer un doublon, pour ne pas orpheliner l'historique qui la référence.
                item = legacyByName.remove(normalize(name));
                if (item != null) {
                    adopted++;
                } else {
                    item = new FoodItem();
                    created++;
                }
            }
            apply(item, code, name, row);
            toSave.add(item);
        }

        // Flush AVANT la suppression : celle-ci s'exécute en SQL brut, hors contexte de
        // persistance. Sans ce flush, les lignes adoptées ont encore ciqual_code NULL en base
        // (il n'est posé qu'en mémoire), le DELETE les emporte, et le commit échoue ensuite en
        // tentant de mettre à jour des lignes disparues.
        foodItemRepository.saveAllAndFlush(toSave);
        int removed = deleteUnreferencedLegacyFoods();
        markApplied();

        log.info("CIQUAL {} : {} aliments ({} créés, {} adoptés, {} mis à jour), {} lignes obsolètes supprimées.",
                csvVersion, toSave.size(), created, adopted, toSave.size() - created - adopted, removed);
    }

    private void apply(FoodItem item, Integer code, String name, Map<String, String> row) {
        item.setCiqualCode(code);
        item.setName(name);
        item.setFoodGroup(emptyToNull(row.get("groupe")));
        item.setFoodSubGroup(emptyToNull(row.get("sous_groupe")));
        item.setSource(FoodSource.CIQUAL);

        // Les macros sont déclarées non nulles en base : une valeur manquante y vaut zéro, ce qui
        // est acceptable pour elles (renseignées à plus de 95 %) mais jamais pour les micros.
        item.setKcal100g(orZero(row.get("kcal")));
        item.setProteins100g(orZero(row.get("proteines")));
        item.setCarbs100g(orZero(row.get("glucides")));
        item.setFats100g(orZero(row.get("lipides")));

        item.setFibers100g(parseValue(row.get("fibres_g")));
        item.setSugars100g(parseValue(row.get("sucres_g")));
        item.setSaturatedFat100g(parseValue(row.get("ags_g")));
        item.setSalt100g(parseValue(row.get("sel_g")));

        Map<Nutrient, Double> micros = new LinkedHashMap<>();
        for (Nutrient nutrient : Nutrient.values()) {
            Double value = parseValue(row.get(nutrient.getCsvColumn()));
            if (value != null) {
                micros.put(nutrient, value);
            }
        }
        item.getMicros100g().clear();
        item.getMicros100g().putAll(micros);
    }

    private List<Map<String, String>> readCsv() {
        List<Map<String, String>> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(RESOURCE).getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                return rows;
            }
            String[] headers = split(headerLine);

            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = split(line);
                if (columns.length != headers.length) {
                    log.warn("Ligne CIQUAL ignorée ({} colonnes au lieu de {}) : {}",
                            columns.length, headers.length, line);
                    continue;
                }
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    row.put(headers[i], columns[i]);
                }
                rows.add(row);
            }
        } catch (Exception e) {
            log.error("Lecture de {} impossible : ", RESOURCE, e);
        }
        return rows;
    }

    /** Retire les guillemets d'échappement CSV, que l'ancien import laissait dans les noms. */
    private String[] split(String line) {
        String[] columns = line.split(CSV_SPLIT, -1);
        for (int i = 0; i < columns.length; i++) {
            String value = columns[i].trim();
            if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1).replace("\"\"", "\"");
            }
            columns[i] = value;
        }
        return columns;
    }

    /**
     * Valeur pour 100 g, ou {@code null} quand la cellule est vide.
     *
     * <p>La distinction est essentielle sur les micronutriments : confondre « non déterminé » et
     * « zéro » ferait afficher des carences imaginaires dès qu'un aliment n'a pas été analysé.
     */
    private Double parseValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Double.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private double orZero(String raw) {
        Double value = parseValue(raw);
        return value != null ? value : 0.0;
    }

    private Integer parseCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Map<String, FoodItem> legacyCiqualFoodsByName() {
        return foodItemRepository.findBySourceAndCiqualCodeIsNull(FoodSource.CIQUAL).stream()
                .collect(Collectors.toMap(item -> normalize(item.getName()), item -> item, (a, b) -> a));
    }

    /**
     * Supprime les aliments CIQUAL de l'ancien import qui n'ont pas trouvé d'équivalent dans le
     * nouveau fichier — pour l'essentiel les plats composés préemballés, désormais écartés. Ceux
     * qu'un journal, un repas ou un planning référence encore sont conservés.
     */
    private int deleteUnreferencedLegacyFoods() {
        return jdbcTemplate.update("""
                DELETE FROM food_items f
                 WHERE f.source = 'CIQUAL' AND f.ciqual_code IS NULL
                   AND NOT EXISTS (SELECT 1 FROM log_entries e WHERE e.food_item_id = f.id)
                   AND NOT EXISTS (SELECT 1 FROM meal_ingredients m WHERE m.food_item_id = f.id)
                   AND NOT EXISTS (SELECT 1 FROM planned_meal_entries p WHERE p.food_item_id = f.id)
                """);
    }

    private String appliedVersion() {
        return appMetadataRepository.findById(VERSION_KEY).map(AppMetadata::getValue).orElse(null);
    }

    private void markApplied() {
        appMetadataRepository.save(new AppMetadata(VERSION_KEY, csvVersion));
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase();
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
