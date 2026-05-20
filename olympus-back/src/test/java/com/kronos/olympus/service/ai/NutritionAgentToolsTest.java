package com.kronos.olympus.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.kronos.olympus.model.DailyLog;
import com.kronos.olympus.model.FoodItem;
import com.kronos.olympus.model.MealPreset;
import com.kronos.olympus.model.User;
import com.kronos.olympus.model.enums.ActivityLevel;
import com.kronos.olympus.model.enums.FoodSource;
import com.kronos.olympus.model.enums.Gender;
import com.kronos.olympus.model.enums.Goal;
import com.kronos.olympus.model.enums.Role;
import com.kronos.olympus.repository.DailyLogRepository;
import com.kronos.olympus.repository.FoodItemRepository;
import com.kronos.olympus.repository.MealPresetRepository;
import com.kronos.olympus.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests d'intégration des outils de l'Oracle : garantit que l'agent peut créer et consulter
 * les repas / le journal de l'utilisateur, et que la recherche privilégie bien CIQUAL.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class NutritionAgentToolsTest {

    @Autowired
    private NutritionAgentTools nutritionAgentTools;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FoodItemRepository foodItemRepository;
    @Autowired
    private MealPresetRepository mealPresetRepository;
    @Autowired
    private DailyLogRepository dailyLogRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    private User user;
    private List<AgentTool> tools;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .email("oracle-test-" + System.nanoTime() + "@olympus.test")
                .passwordHash("x")
                .role(Role.USER)
                .gender(Gender.MALE)
                .heightCm(180.0)
                .currentWeightKg(75.0)
                .birthDate(LocalDate.of(1995, 1, 1))
                .activityLevel(ActivityLevel.MODERATE)
                .goal(Goal.MAINTAIN)
                .build());
        tools = nutritionAgentTools.buildTools(user);
    }

    private String invoke(String toolName, String jsonArgs) throws Exception {
        AgentTool tool = tools.stream()
                .filter(t -> t.getName().equals(toolName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Outil introuvable : " + toolName));
        return tool.execute(objectMapper.readTree(jsonArgs));
    }

    private FoodItem seedFood(String name, FoodSource source) {
        return foodItemRepository.saveAndFlush(FoodItem.builder()
                .name(name)
                .barcode(source.name() + "-" + name + "-" + System.nanoTime())
                .kcal100g(120.0)
                .proteins100g(8.0)
                .carbs100g(15.0)
                .fats100g(3.0)
                .source(source)
                .build());
    }

    /**
     * Recharge le journal du jour depuis la base : vide d'abord le contexte de persistance
     * pour ne pas récupérer une entité gérée dont la collection {@code entries} serait obsolète.
     */
    private DailyLog todayLog() {
        entityManager.flush();
        entityManager.clear();
        return dailyLogRepository.findByUserIdAndTargetDate(user.getId(), LocalDate.now())
                .orElseThrow(() -> new AssertionError("Aucun journal pour aujourd'hui"));
    }

    @Test
    void createMealPreset_persistsPresetWithIngredients() throws Exception {
        seedFood("Zorpquinoa", FoodSource.CIQUAL);

        String result = invoke("create_meal_preset",
                "{\"name\":\"Test Bowl\",\"ingredients\":[{\"foodName\":\"Zorpquinoa\",\"quantityGrams\":100}]}");

        assertTrue(result.toLowerCase().contains("créé"), "Réponse inattendue : " + result);
        List<MealPreset> presets = mealPresetRepository.findAllByUserId(user.getId());
        assertEquals(1, presets.size());
        assertEquals("Test Bowl", presets.get(0).getName());
        assertEquals(1, presets.get(0).getIngredients().size());
    }

    @Test
    void getMealPresets_listsCreatedPreset() throws Exception {
        seedFood("Zorpquinoa", FoodSource.CIQUAL);
        invoke("create_meal_preset",
                "{\"name\":\"Bol Olympien\",\"ingredients\":[{\"foodName\":\"Zorpquinoa\",\"quantityGrams\":100}]}");

        String result = invoke("get_meal_presets", "{}");
        assertTrue(result.contains("Bol Olympien"), "Réponse inattendue : " + result);
    }

    @Test
    void logFood_logsFoodAndUpdatesDailyTotals() throws Exception {
        seedFood("Zorpavoine", FoodSource.OFF);

        String result = invoke("log_food",
                "{\"foodName\":\"Zorpavoine\",\"quantityGrams\":200}");

        assertTrue(result.toLowerCase().contains("ajouté"), "Réponse inattendue : " + result);
        DailyLog log = todayLog();
        assertTrue(log.getTotalKcal() > 0, "Les totaux du jour devraient être mis à jour");
        assertEquals(1, log.getEntries().size());
    }

    @Test
    void logFood_prefersCiqualOverOpenFoodFacts() throws Exception {
        // Même nom dans les deux bases : l'Oracle doit choisir l'entrée CIQUAL
        seedFood("Zorpmillet", FoodSource.OFF);
        seedFood("Zorpmillet", FoodSource.CIQUAL);

        invoke("log_food", "{\"foodName\":\"Zorpmillet\",\"quantityGrams\":100}");

        DailyLog log = todayLog();
        assertEquals(1, log.getEntries().size());
        assertEquals(FoodSource.CIQUAL, log.getEntries().get(0).getFoodItem().getSource(),
                "L'aliment journalisé devrait provenir de CIQUAL");
    }

    @Test
    void logEstimatedFood_createsAiFoodAndLogsIt() throws Exception {
        String result = invoke("log_estimated_food",
                "{\"name\":\"Plat Mystere XYZ\",\"quantityGrams\":250,\"kcalPer100g\":180,"
                        + "\"proteinsPer100g\":10,\"carbsPer100g\":20,\"fatsPer100g\":7}");

        assertTrue(result.toLowerCase().contains("estimation"), "Réponse inattendue : " + result);
        assertFalse(foodItemRepository.searchByNameOrderedByLength("Plat Mystere XYZ").isEmpty(),
                "Un FoodItem estimé devrait avoir été créé");
        assertTrue(todayLog().getTotalKcal() > 0);
    }

    @Test
    void deleteLogEntry_removesEntryFromJournal() throws Exception {
        seedFood("Zorpavoine", FoodSource.OFF);
        invoke("log_food", "{\"foodName\":\"Zorpavoine\",\"quantityGrams\":150}");

        Long entryId = todayLog().getEntries().get(0).getId();
        String result = invoke("delete_log_entry", "{\"entryId\":" + entryId + "}");

        assertTrue(result.toLowerCase().contains("supprimée"), "Réponse inattendue : " + result);
        assertTrue(todayLog().getEntries().isEmpty(), "L'entrée devrait être supprimée");
    }

    @Test
    void deleteMealPreset_removesPreset() throws Exception {
        seedFood("Zorpquinoa", FoodSource.CIQUAL);
        invoke("create_meal_preset",
                "{\"name\":\"Repas Jetable\",\"ingredients\":[{\"foodName\":\"Zorpquinoa\",\"quantityGrams\":100}]}");
        Long presetId = mealPresetRepository.findAllByUserId(user.getId()).get(0).getId();

        invoke("delete_meal_preset", "{\"mealPresetId\":" + presetId + "}");

        assertTrue(mealPresetRepository.findAllByUserId(user.getId()).isEmpty(),
                "Le repas pré-enregistré devrait être supprimé");
    }
}
