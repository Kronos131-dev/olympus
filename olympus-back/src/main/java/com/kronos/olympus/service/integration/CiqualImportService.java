package com.kronos.olympus.service.integration;

import com.kronos.olympus.model.FoodItem;
import com.kronos.olympus.model.enums.FoodSource;
import com.kronos.olympus.repository.FoodItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CiqualImportService implements CommandLineRunner {

    private final FoodItemRepository foodItemRepository;

    @Override
    public void run(String... args) {
        // 1. Vérifier si les données sont déjà en base
        if (foodItemRepository.existsBySource(FoodSource.CIQUAL)) {
            log.info("✅ Données CIQUAL déjà présentes en base. Import ignoré.");
            return;
        }

        log.info("⏳ Démarrage de l'import des données CIQUAL...");
        List<FoodItem> foodItems = new ArrayList<>();

        // 2. Lecture du fichier CSV (placé dans src/main/resources/ciqual.csv)
        // Format attendu (séparateur ;) : nom;kcal;proteines;glucides;lipides
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ClassPathResource("ciqual.csv").getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false; // On saute la ligne des en-têtes
                    continue;
                }

                String[] columns = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                // On s'assure d'avoir au moins nos 5 colonnes de base
                if (columns.length >= 5) {
                    try {
                        FoodItem item = FoodItem.builder()
                                .name(columns[0].trim())
                                .kcal100g(parseCiqualValue(columns[1]))
                                .proteins100g(parseCiqualValue(columns[2]))
                                .carbs100g(parseCiqualValue(columns[3]))
                                .fats100g(parseCiqualValue(columns[4]))
                                .source(FoodSource.CIQUAL)
                                .build();

                        foodItems.add(item);
                    } catch (Exception e) {
                        log.warn("⚠️ Erreur de parsing pour la ligne : {} - {}", line, e.getMessage());
                    }
                }
            }

            // 3. Sauvegarde en batch pour optimiser les performances
            foodItemRepository.saveAll(foodItems);
            log.info("🚀 Import réussi ! {} aliments CIQUAL ajoutés à la base.", foodItems.size());

        } catch (Exception e) {
            log.error("❌ Erreur critique lors de la lecture du fichier ciqual.csv : ", e);
        }
    }

    /**
     * Méthode utilitaire pour nettoyer les valeurs CIQUAL.
     * Remplace les virgules par des points, gère les mentions "< 0.5" ou "traces".
     */
    private Double parseCiqualValue(String value) {
        if (value == null || value.trim().isEmpty() || value.equalsIgnoreCase("traces") || value.equals("-")) {
            return 0.0;
        }

        // Nettoyage des symboles et remplacement de la virgule française
        String cleanValue = value.replace("\"", "")
                .replace("<", "")
                .replace(">", "")
                .replace(",", ".")
                .trim();
        try {
            return Double.parseDouble(cleanValue);
        } catch (NumberFormatException e) {
            return 0.0; // Fallback sécurisé si la donnée est inexploitable
        }
    }
}