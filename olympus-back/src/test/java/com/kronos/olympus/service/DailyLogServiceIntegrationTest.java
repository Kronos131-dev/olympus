package com.kronos.olympus.service;

import com.kronos.olympus.dto.request.LogEntryRequest;
import com.kronos.olympus.dto.response.DailyLogResponse;
import com.kronos.olympus.model.FoodItem;
import com.kronos.olympus.model.MealIngredient;
import com.kronos.olympus.model.MealPreset;
import com.kronos.olympus.model.User;
import com.kronos.olympus.model.enums.ActivityLevel;
import com.kronos.olympus.model.enums.FoodSource;
import com.kronos.olympus.model.enums.Gender;
import com.kronos.olympus.model.enums.Goal;
import com.kronos.olympus.model.enums.Role;
import com.kronos.olympus.repository.FoodItemRepository;
import com.kronos.olympus.repository.MealPresetRepository;
import com.kronos.olympus.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vérifie que l'ajout d'une entrée au journal renvoie une réponse qui CONTIENT la nouvelle
 * entrée (et pas seulement les totaux). Régression : addLogEntry sauvegardait la LogEntry mais
 * ne l'ajoutait pas à dailyLog.getEntries(), si bien que l'entrée n'apparaissait pas dans
 * l'historique tant qu'un refetch n'avait pas eu lieu.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class DailyLogServiceIntegrationTest {

    @Autowired
    private DailyLogService dailyLogService;
    @Autowired
    private MealPresetService mealPresetService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FoodItemRepository foodItemRepository;
    @Autowired
    private MealPresetRepository mealPresetRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private User user;

    private FoodItem seedFood() {
        return foodItemRepository.saveAndFlush(FoodItem.builder()
                .name("Zorpavoine")
                .barcode("CIQUAL-Zorpavoine-" + System.nanoTime())
                .kcal100g(100.0).proteins100g(10.0).carbs100g(20.0).fats100g(5.0)
                .source(FoodSource.CIQUAL)
                .build());
    }

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .email("dailylog-test-" + System.nanoTime() + "@olympus.test")
                .passwordHash("x")
                .role(Role.USER)
                .gender(Gender.MALE)
                .heightCm(180.0)
                .currentWeightKg(75.0)
                .birthDate(LocalDate.of(1995, 1, 1))
                .activityLevel(ActivityLevel.MODERATE)
                .goal(Goal.MAINTAIN)
                .build());
    }

    @Test
    void addLogEntry_returnsResponseContainingTheNewEntry() {
        FoodItem food = foodItemRepository.saveAndFlush(FoodItem.builder()
                .name("Zorpavoine")
                .barcode("CIQUAL-Zorpavoine-" + System.nanoTime())
                .kcal100g(100.0).proteins100g(10.0).carbs100g(20.0).fats100g(5.0)
                .source(FoodSource.CIQUAL)
                .build());

        LogEntryRequest request = LogEntryRequest.builder()
                .targetDate(LocalDate.now())
                .foodItemId(food.getId())
                .quantityGrams(200.0)
                .build();

        DailyLogResponse response = dailyLogService.addLogEntry(user, request);

        assertTrue(response.getTotalKcal() > 0, "Les totaux doivent être mis à jour");
        assertEquals(1, response.getEntries().size(),
                "La réponse doit contenir la nouvelle entrée");
        assertEquals(food.getId(), response.getEntries().get(0).getFoodItem().getId());
    }

    @Test
    void removeLogEntry_resetsTotalsToZero() {
        FoodItem food = seedFood();
        DailyLogResponse added = dailyLogService.addLogEntry(user, LogEntryRequest.builder()
                .targetDate(LocalDate.now()).foodItemId(food.getId()).quantityGrams(200.0).build());
        Long entryId = added.getEntries().get(0).getId();

        DailyLogResponse afterRemove = dailyLogService.removeLogEntry(user, entryId);

        assertEquals(0.0, afterRemove.getTotalKcal(), "Les totaux doivent retomber à 0");
        assertTrue(afterRemove.getEntries().isEmpty(), "L'historique doit être vide");
    }

    @Test
    void deletingLoggedPreset_leavesNoPhantomCalories() {
        FoodItem food = seedFood();
        // Repas pré-enregistré (200 g de l'aliment) puis log de ce repas dans la journée.
        MealPreset preset = MealPreset.builder().user(user).name("Bol").build();
        preset.setIngredients(new ArrayList<>(List.of(MealIngredient.builder()
                .mealPreset(preset).foodItem(food).quantityGrams(200.0).build())));
        preset = mealPresetRepository.saveAndFlush(preset);

        DailyLogResponse added = dailyLogService.addLogEntry(user, LogEntryRequest.builder()
                .targetDate(LocalDate.now()).mealPresetId(preset.getId()).build());
        assertTrue(added.getTotalKcal() > 0, "Le repas loggé doit compter des calories");

        // Suppression du repas pré-enregistré (détache l'entrée de journal : mealPreset = null).
        mealPresetService.deleteMealPreset(preset.getId(), user.getId());
        // Vider le contexte pour simuler une requête fraîche (la détache est une requête bulk).
        entityManager.flush();
        entityManager.clear();

        DailyLogResponse after = dailyLogService.getDailyLogByDate(user, LocalDate.now());

        assertEquals(0.0, after.getTotalKcal(),
                "Aucune calorie fantôme : les totaux sont recalculés depuis les entrées");
        assertEquals(0.0, after.getTotalCarbs());
    }
}
