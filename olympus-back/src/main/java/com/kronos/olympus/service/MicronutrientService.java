package com.kronos.olympus.service;

import com.kronos.olympus.dto.response.DailyMicronutrientsResponse;
import com.kronos.olympus.dto.response.MicronutrientResponse;
import com.kronos.olympus.model.DailyLog;
import com.kronos.olympus.model.FoodItem;
import com.kronos.olympus.model.LogEntry;
import com.kronos.olympus.model.MealIngredient;
import com.kronos.olympus.model.User;
import com.kronos.olympus.model.enums.Nutrient;
import com.kronos.olympus.repository.DailyLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Bilan quotidien des micronutriments, calculé à la volée depuis les entrées du journal.
 *
 * <p>Contrairement aux macronutriments, ces valeurs ne sont pas stockées sur {@code DailyLog} :
 * elles ne sont connues que pour les aliments issus de CIQUAL, si bien qu'un total seul serait
 * trompeur. Le service renvoie donc, pour chaque nutriment, la part des calories du jour
 * provenant d'aliments qui le renseignent — un produit scanné ou une estimation de l'IA fait
 * baisser cette couverture, et l'écran peut alors nuancer au lieu d'annoncer une carence.
 */
@Service
@RequiredArgsConstructor
public class MicronutrientService {

    private final DailyLogRepository dailyLogRepository;

    @Transactional(readOnly = true)
    public DailyMicronutrientsResponse getDailyMicronutrients(User user, LocalDate date) {
        Map<Nutrient, Double> consumed = new EnumMap<>(Nutrient.class);
        Map<Nutrient, Double> coveredKcal = new EnumMap<>(Nutrient.class);
        double totalKcal = 0;

        DailyLog dailyLog = dailyLogRepository.findByUserIdAndTargetDate(user.getId(), date).orElse(null);
        if (dailyLog != null && dailyLog.getEntries() != null) {
            for (LogEntry entry : dailyLog.getEntries()) {
                if (entry.getFoodItem() != null && entry.getQuantityGrams() != null) {
                    totalKcal += accumulate(entry.getFoodItem(), entry.getQuantityGrams(), consumed, coveredKcal);
                } else if (entry.getMealPreset() != null && entry.getMealPreset().getIngredients() != null) {
                    for (MealIngredient ingredient : entry.getMealPreset().getIngredients()) {
                        if (ingredient.getFoodItem() != null && ingredient.getQuantityGrams() != null) {
                            totalKcal += accumulate(ingredient.getFoodItem(), ingredient.getQuantityGrams(),
                                    consumed, coveredKcal);
                        }
                    }
                }
            }
        }

        List<MicronutrientResponse> nutrients = new ArrayList<>();
        double coverageSum = 0;
        for (Nutrient nutrient : Nutrient.values()) {
            double coverage = totalKcal > 0 ? coveredKcal.getOrDefault(nutrient, 0.0) / totalKcal : 0.0;
            coverageSum += coverage;
            nutrients.add(MicronutrientResponse.builder()
                    .nutrient(nutrient)
                    .category(nutrient.getCategory())
                    .unit(nutrient.getUnit())
                    .consumed(round(consumed.getOrDefault(nutrient, 0.0)))
                    .reference(nutrient.referenceFor(user.getGender()))
                    .coverage(round(coverage))
                    .build());
        }

        return DailyMicronutrientsResponse.builder()
                .targetDate(date)
                .nutrients(nutrients)
                .overallCoverage(round(coverageSum / Nutrient.values().length))
                .build();
    }

    /** Ajoute la contribution d'un aliment et renvoie les calories qu'il apporte. */
    private double accumulate(FoodItem food, double grams,
                              Map<Nutrient, Double> consumed, Map<Nutrient, Double> coveredKcal) {
        double ratio = grams / 100.0;
        double kcal = food.getKcal100g() != null ? food.getKcal100g() * ratio : 0.0;

        food.getMicros100g().forEach((nutrient, per100g) -> {
            consumed.merge(nutrient, per100g * ratio, Double::sum);
            coveredKcal.merge(nutrient, kcal, Double::sum);
        });
        return kcal;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
