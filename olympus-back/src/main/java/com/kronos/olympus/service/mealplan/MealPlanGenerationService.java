package com.kronos.olympus.service.mealplan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kronos.olympus.dto.request.PlannedMealEntryRequest;
import com.kronos.olympus.dto.request.WeeklyPlanRequest;
import com.kronos.olympus.dto.response.MealPlanResponse;
import com.kronos.olympus.exception.ExternalApiException;
import com.kronos.olympus.model.FoodItem;
import com.kronos.olympus.model.User;
import com.kronos.olympus.model.enums.AiProvider;
import com.kronos.olympus.model.enums.FoodSource;
import com.kronos.olympus.repository.FoodItemRepository;
import com.kronos.olympus.service.ai.AgentClient;
import com.kronos.olympus.service.ai.AgentResult;
import com.kronos.olympus.service.ai.GeminiAgentClient;
import com.kronos.olympus.service.ai.MistralAgentClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Génération de l'emploi du temps hebdomadaire par l'IA. Service partagé par les deux
 * surfaces : l'endpoint REST {@code POST /meal-plans/generate} (qui appelle lui-même le LLM)
 * et l'outil conversationnel {@code create_meal_plan} (le LLM fournit déjà le plan structuré).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MealPlanGenerationService {

    private final FoodItemRepository foodItemRepository;
    private final MealPlanService mealPlanService;
    private final MistralAgentClient mistralAgentClient;
    private final GeminiAgentClient geminiAgentClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            Tu es un nutritionniste qui génère des plans de repas hebdomadaires.
            Réponds UNIQUEMENT avec un tableau JSON, sans aucun texte ni balise markdown autour.
            Chaque élément du tableau est un repas planifié avec EXACTEMENT ces champs :
            - "day" : jour en anglais majuscules (MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY)
            - "name" : nom du plat ou de l'aliment, en français
            - "quantityGrams" : quantité en grammes (nombre)
            - "kcalPer100g" : calories pour 100g (nombre)
            - "proteinsPer100g" : protéines en grammes pour 100g (nombre)
            - "carbsPer100g" : glucides en grammes pour 100g (nombre)
            - "fatsPer100g" : lipides en grammes pour 100g (nombre)
            Couvre les 7 jours de la semaine, avec plusieurs repas par jour (petit-déjeuner, déjeuner, dîner).
            """;

    /** Correspondance des jours en français : repli si le LLM dévie de la consigne anglaise. */
    private static final Map<String, DayOfWeek> FRENCH_DAYS = Map.of(
            "LUNDI", DayOfWeek.MONDAY,
            "MARDI", DayOfWeek.TUESDAY,
            "MERCREDI", DayOfWeek.WEDNESDAY,
            "JEUDI", DayOfWeek.THURSDAY,
            "VENDREDI", DayOfWeek.FRIDAY,
            "SAMEDI", DayOfWeek.SATURDAY,
            "DIMANCHE", DayOfWeek.SUNDAY);

    /** Génère un emploi du temps hebdomadaire via l'IA à partir des préférences de l'utilisateur. */
    @Transactional
    public MealPlanResponse generateWeeklyPlan(User user, String preferences) {
        AgentClient client = pickClient(user);
        String userPrompt = (preferences == null || preferences.isBlank())
                ? "Génère un plan de repas hebdomadaire équilibré et varié."
                : preferences.trim();

        AgentResult result = client.run(SYSTEM_PROMPT, List.of(), userPrompt, null, null, List.of());

        List<GeneratedMeal> meals = parseMeals(extractArray(parseJson(result.getReply())));
        if (meals.isEmpty()) {
            throw new ExternalApiException(
                    "L'IA n'a pas pu générer de plan exploitable. Reformule ta demande avec plus de détails.");
        }
        return persist(user, meals);
    }

    /** Applique un plan déjà structuré par l'agent conversationnel (outil create_meal_plan). */
    @Transactional
    public MealPlanResponse applyGeneratedPlan(User user, JsonNode mealsArray) {
        List<GeneratedMeal> meals = parseMeals(mealsArray);
        if (meals.isEmpty()) {
            throw new IllegalArgumentException("Le plan fourni ne contient aucun repas valide.");
        }
        return persist(user, meals);
    }

    /**
     * Crée un {@link FoodItem} d'origine IA pour chaque repas généré, puis enregistre le tout
     * comme l'emploi du temps hebdomadaire de l'utilisateur.
     */
    private MealPlanResponse persist(User user, List<GeneratedMeal> meals) {
        List<PlannedMealEntryRequest> entries = new ArrayList<>();
        for (GeneratedMeal meal : meals) {
            FoodItem food = foodItemRepository.save(FoodItem.builder()
                    .name("IA : " + meal.name())
                    .kcal100g(meal.kcalPer100g())
                    .proteins100g(meal.proteinsPer100g())
                    .carbs100g(meal.carbsPer100g())
                    .fats100g(meal.fatsPer100g())
                    .source(FoodSource.AI)
                    .barcode("AI-" + UUID.randomUUID().toString().substring(0, 8))
                    .build());
            entries.add(PlannedMealEntryRequest.builder()
                    .foodItemId(food.getId())
                    .quantityGrams(meal.quantityGrams())
                    .dayOfWeek(meal.dayOfWeek())
                    .build());
        }
        return mealPlanService.saveWeeklyPlan(user, WeeklyPlanRequest.builder().entries(entries).build());
    }

    private AgentClient pickClient(User user) {
        if (user.getAiProvider() == AiProvider.GEMINI && geminiAgentClient.isConfigured()) {
            return geminiAgentClient;
        }
        if (mistralAgentClient.isConfigured()) {
            return mistralAgentClient;
        }
        if (geminiAgentClient.isConfigured()) {
            return geminiAgentClient;
        }
        throw new ExternalApiException("Aucun fournisseur d'IA n'est configuré côté serveur.");
    }

    /** Parse la réponse texte du LLM en JSON, en tolérant markdown et texte parasite. */
    private JsonNode parseJson(String reply) {
        String text = reply == null ? "" : reply.trim();
        if (text.startsWith("```")) {
            int firstLineBreak = text.indexOf('\n');
            if (firstLineBreak >= 0) {
                text = text.substring(firstLineBreak + 1);
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
        }
        text = text.trim();
        try {
            return objectMapper.readTree(text);
        } catch (Exception firstAttempt) {
            // Repli : le LLM a parfois encadré le tableau de phrases d'introduction.
            // On isole la sous-chaîne du premier '[' au dernier ']' et on retente.
            int start = text.indexOf('[');
            int end = text.lastIndexOf(']');
            if (start >= 0 && end > start) {
                try {
                    return objectMapper.readTree(text.substring(start, end + 1));
                } catch (Exception ignored) {
                    // Échec définitif traité ci-dessous.
                }
            }
            log.warn("Réponse IA non parsable en JSON : {}", reply);
            throw new ExternalApiException("L'IA a renvoyé un plan illisible. Réessaie.");
        }
    }

    /** Extrait le tableau de repas, que la racine soit directement un tableau ou un objet l'enveloppant. */
    private JsonNode extractArray(JsonNode root) {
        if (root.isArray()) {
            return root;
        }
        if (root.isObject()) {
            for (JsonNode child : root) {
                if (child.isArray()) {
                    return child;
                }
            }
        }
        return objectMapper.createArrayNode();
    }

    private List<GeneratedMeal> parseMeals(JsonNode array) {
        List<GeneratedMeal> meals = new ArrayList<>();
        if (array == null || !array.isArray()) {
            return meals;
        }
        for (JsonNode node : array) {
            DayOfWeek day = parseDay(node.path("day").asText(""));
            String name = node.path("name").asText("").trim();
            if (day == null || name.isEmpty()) {
                continue;
            }
            double qty = node.path("quantityGrams").asDouble(0);
            meals.add(new GeneratedMeal(
                    day,
                    name,
                    qty > 0 ? qty : 100.0,
                    Math.max(0, node.path("kcalPer100g").asDouble(0)),
                    Math.max(0, node.path("proteinsPer100g").asDouble(0)),
                    Math.max(0, node.path("carbsPer100g").asDouble(0)),
                    Math.max(0, node.path("fatsPer100g").asDouble(0))));
        }
        return meals;
    }

    private DayOfWeek parseDay(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        try {
            return DayOfWeek.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Repli : le LLM a parfois répondu en français malgré la consigne.
            return FRENCH_DAYS.get(normalized);
        }
    }

    /** Un repas planifié produit par l'IA, avant persistance. */
    private record GeneratedMeal(DayOfWeek dayOfWeek, String name, double quantityGrams,
                                 double kcalPer100g, double proteinsPer100g,
                                 double carbsPer100g, double fatsPer100g) {
    }
}
