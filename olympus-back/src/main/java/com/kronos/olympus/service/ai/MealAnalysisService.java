package com.kronos.olympus.service.ai;

import com.kronos.olympus.dto.request.AnalyzedFoodRequest;
import com.kronos.olympus.dto.request.MealConfirmationRequest;
import com.kronos.olympus.dto.request.MealCorrectionRequest;
import com.kronos.olympus.dto.response.AnalyzedFoodResponse;
import com.kronos.olympus.dto.response.DailyLogResponse;
import com.kronos.olympus.dto.response.MealAnalysisResponse;
import com.kronos.olympus.dto.request.LogEntryRequest;
import com.kronos.olympus.exception.ExternalApiException;
import com.kronos.olympus.model.FoodItem;
import com.kronos.olympus.model.User;
import com.kronos.olympus.model.enums.AiProvider;
import com.kronos.olympus.model.enums.FoodSource;
import com.kronos.olympus.model.enums.Nutrient;
import com.kronos.olympus.repository.FoodItemRepository;
import com.kronos.olympus.service.DailyLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Analyse structurée d'un repas, à partir d'une photo ou d'une description.
 *
 * <p>Le modèle ne fournit que ce qu'il sait vraiment faire : nommer les aliments visibles et
 * estimer leur poids. Les valeurs nutritionnelles viennent ensuite du référentiel CIQUAL, ce qui
 * les rend reproductibles et apporte les micronutriments — un LLM interrogé sur le magnésium d'une
 * assiette invente un chiffre différent à chaque appel. L'estimation par le modèle ne sert que de
 * repli, pour les aliments introuvables en base, et n'apporte alors aucun micronutriment.
 *
 * <p>Distinct de l'agent conversationnel : {@link AgentService} tient une conversation et répond
 * en prose, ce service rend un objet exploitable par l'écran d'analyse.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MealAnalysisService {

    private static final int MAX_ITEMS = 20;

    private static final String DETECTION_PROMPT = """
            Tu es un expert en nutrition qui inventorie le contenu d'un repas.
            Identifie CHAQUE aliment distinct et estime son poids en grammes, en t'appuyant sur les
            repères visuels disponibles (taille de l'assiette, couverts, mains) quand il y a une photo.
            Décompose les plats : une pizza devient pâte, sauce tomate, fromage et garniture ; un
            sandwich devient pain, beurre, jambon, salade. N'invente pas d'aliment que rien n'indique.
            Nomme chaque aliment avec des termes simples et courants en français, en précisant l'état
            (cru, cuit, grillé) car il change la composition : « riz blanc cuit », « blanc de poulet grillé ».

            Réponds UNIQUEMENT avec un objet JSON, sans texte ni balise markdown autour :
            {"mealName": "nom court du repas en français",
             "items": [{"name": "nom de l'aliment", "quantityGrams": nombre}]}
            """;

    private static final String CORRECTION_PROMPT = """
            Tu corriges l'inventaire d'un repas à partir d'une remarque de l'utilisateur.
            On te donne la liste actuelle des aliments et leurs quantités, puis sa correction.
            Applique EXACTEMENT ce qu'il demande : ajuster une quantité, retirer un aliment, en
            ajouter un, en renommer un. Ne touche à rien d'autre et conserve l'ordre existant.

            Réponds UNIQUEMENT avec un objet JSON, sans texte ni balise markdown autour :
            {"mealName": "nom court du repas en français",
             "items": [{"name": "nom de l'aliment", "quantityGrams": nombre}]}
            """;

    private static final String ESTIMATION_PROMPT = """
            Tu estimes la composition nutritionnelle d'aliments absents des tables de référence.
            Pour chaque aliment fourni, donne ses valeurs POUR 100 g.

            Réponds UNIQUEMENT avec un tableau JSON, sans texte ni balise markdown autour :
            [{"name": "nom repris à l'identique", "kcalPer100g": nombre, "proteinsPer100g": nombre,
              "carbsPer100g": nombre, "fatsPer100g": nombre}]
            """;

    private final MistralAgentClient mistralAgentClient;
    private final GeminiAgentClient geminiAgentClient;
    private final FoodItemRepository foodItemRepository;
    private final DailyLogService dailyLogService;
    private final ObjectMapper objectMapper;

    @Transactional
    public MealAnalysisResponse analyzePhoto(User user, MultipartFile image, String note) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Aucune photo reçue.");
        }
        // Le modèle Mistral configuré n'est pas multimodal : sans Gemini, mieux vaut refuser que
        // renvoyer une analyse muette ou inventée à partir du seul commentaire.
        if (!geminiAgentClient.isConfigured()) {
            throw new ExternalApiException(
                    "L'analyse par photo nécessite Gemini, qui n'est pas configuré sur ce serveur. "
                            + "Décris ton repas pour obtenir une estimation.");
        }

        String base64;
        try {
            base64 = Base64.getEncoder().encodeToString(image.getBytes());
        } catch (IOException e) {
            throw new IllegalArgumentException("Impossible de lire la photo envoyée.");
        }
        String mime = image.getContentType() != null ? image.getContentType() : "image/jpeg";
        String userText = (note == null || note.isBlank())
                ? "Inventorie ce repas."
                : "Inventorie ce repas. Précision de l'utilisateur : " + note.trim();

        AgentResult result = geminiAgentClient.run(DETECTION_PROMPT, List.of(), userText, base64, mime, List.of());
        return resolve(readAnalysis(result.getReply()), user);
    }

    @Transactional
    public MealAnalysisResponse analyzeText(User user, String description) {
        AgentResult result = pickClient(user).run(
                DETECTION_PROMPT, List.of(), "Repas décrit : " + description.trim(), null, null, List.of());
        return resolve(readAnalysis(result.getReply()), user);
    }

    @Transactional
    public MealAnalysisResponse correct(User user, MealCorrectionRequest request) {
        StringBuilder prompt = new StringBuilder("Inventaire actuel :\n");
        for (AnalyzedFoodRequest item : request.getItems()) {
            prompt.append("- ").append(item.getName())
                    .append(" : ").append(Math.round(orZero(item.getQuantityGrams()))).append(" g\n");
        }
        prompt.append("\nCorrection demandée : ").append(request.getCorrection().trim());

        AgentResult result = pickClient(user).run(
                CORRECTION_PROMPT, List.of(), prompt.toString(), null, null, List.of());
        return resolve(readAnalysis(result.getReply()), user);
    }

    /** Enregistre une entrée de journal par aliment retenu, plutôt qu'un unique bloc agrégé. */
    @Transactional
    public DailyLogResponse confirm(User user, MealConfirmationRequest request) {
        LocalDate date = request.getTargetDate() != null ? request.getTargetDate() : LocalDate.now();
        DailyLogResponse log = null;
        for (AnalyzedFoodRequest item : request.getItems()) {
            if (item.getFoodItemId() == null || orZero(item.getQuantityGrams()) <= 0) {
                continue;
            }
            log = dailyLogService.addLogEntry(user, LogEntryRequest.builder()
                    .targetDate(date)
                    .foodItemId(item.getFoodItemId())
                    .quantityGrams(item.getQuantityGrams())
                    .build());
        }
        if (log == null) {
            throw new IllegalArgumentException("Aucun aliment valide à enregistrer.");
        }
        return log;
    }

    // ---- Résolution des aliments détectés ----

    /**
     * Associe chaque aliment détecté à une ligne du référentiel, puis met ses valeurs à l'échelle
     * du poids estimé. Les aliments sans correspondance partent en une seule requête d'estimation
     * groupée, pour ne pas multiplier les allers-retours avec le modèle.
     */
    private MealAnalysisResponse resolve(DetectedMeal meal, User user) {
        Map<String, FoodItem> resolved = new LinkedHashMap<>();
        List<String> unmatched = new ArrayList<>();

        for (DetectedFood detected : meal.items()) {
            FoodItem match = findInReferenceTable(detected.name());
            if (match != null) {
                resolved.put(detected.name(), match);
            } else {
                unmatched.add(detected.name());
            }
        }
        if (!unmatched.isEmpty()) {
            resolved.putAll(estimateMissing(unmatched, user));
        }

        List<AnalyzedFoodResponse> items = new ArrayList<>();
        for (DetectedFood detected : meal.items()) {
            FoodItem food = resolved.get(detected.name());
            if (food != null) {
                items.add(scale(food, detected.quantityGrams()));
            }
        }
        return totals(meal.mealName(), items);
    }

    private FoodItem findInReferenceTable(String name) {
        List<FoodItem> ciqual = foodItemRepository.searchSmartCiqual(name, FoodSource.CIQUAL.name());
        if (!ciqual.isEmpty()) {
            return ciqual.get(0);
        }
        // Repli sur le cache Open Food Facts déjà constitué : moins riche en micronutriments,
        // mais plus fiable que de laisser le modèle inventer les macros.
        List<FoodItem> cached = foodItemRepository.searchByNameOrderedByLength(name);
        return cached.isEmpty() ? null : cached.get(0);
    }

    /** Demande au modèle les valeurs pour 100 g des aliments absents du référentiel. */
    private Map<String, FoodItem> estimateMissing(List<String> names, User user) {
        Map<String, FoodItem> estimated = new LinkedHashMap<>();
        AgentResult result = pickClient(user).run(
                ESTIMATION_PROMPT, List.of(), "Aliments : " + String.join(", ", names), null, null, List.of());

        JsonNode array = extractArray(parseJson(result.getReply()));
        Map<String, JsonNode> byName = new HashMap<>();
        for (JsonNode node : array) {
            byName.put(normalize(node.path("name").asText("")), node);
        }

        for (String name : names) {
            JsonNode node = byName.get(normalize(name));
            if (node == null) {
                log.warn("Aliment estimé manquant dans la réponse du modèle : {}", name);
                continue;
            }
            estimated.put(name, foodItemRepository.save(FoodItem.builder()
                    .name("IA : " + name)
                    .kcal100g(positive(node, "kcalPer100g"))
                    .proteins100g(positive(node, "proteinsPer100g"))
                    .carbs100g(positive(node, "carbsPer100g"))
                    .fats100g(positive(node, "fatsPer100g"))
                    .source(FoodSource.AI)
                    .barcode("AI-" + UUID.randomUUID().toString().substring(0, 8))
                    .build()));
        }
        return estimated;
    }

    private AnalyzedFoodResponse scale(FoodItem food, double grams) {
        double ratio = grams / 100.0;
        Map<Nutrient, Double> micros = new EnumMap<>(Nutrient.class);
        food.getMicros100g().forEach((nutrient, value) -> micros.put(nutrient, round(value * ratio)));

        return AnalyzedFoodResponse.builder()
                .name(food.getName())
                .quantityGrams(round(grams))
                .source(food.getSource())
                .foodItemId(food.getId())
                .kcal(round(orZero(food.getKcal100g()) * ratio))
                .proteins(round(orZero(food.getProteins100g()) * ratio))
                .carbs(round(orZero(food.getCarbs100g()) * ratio))
                .fats(round(orZero(food.getFats100g()) * ratio))
                .fibers(scaleNullable(food.getFibers100g(), ratio))
                .sugars(scaleNullable(food.getSugars100g(), ratio))
                .saturatedFat(scaleNullable(food.getSaturatedFat100g(), ratio))
                .salt(scaleNullable(food.getSalt100g(), ratio))
                .micros(micros)
                .build();
    }

    private MealAnalysisResponse totals(String mealName, List<AnalyzedFoodResponse> items) {
        Map<Nutrient, Double> micros = new EnumMap<>(Nutrient.class);
        double kcal = 0;
        double kcalWithMicros = 0;
        for (AnalyzedFoodResponse item : items) {
            kcal += orZero(item.getKcal());
            if (!item.getMicros().isEmpty()) {
                kcalWithMicros += orZero(item.getKcal());
            }
            item.getMicros().forEach((nutrient, value) -> micros.merge(nutrient, value, Double::sum));
        }
        micros.replaceAll((nutrient, value) -> round(value));

        return MealAnalysisResponse.builder()
                .mealName(mealName)
                .items(items)
                .totalKcal(round(kcal))
                .totalProteins(round(sum(items, AnalyzedFoodResponse::getProteins)))
                .totalCarbs(round(sum(items, AnalyzedFoodResponse::getCarbs)))
                .totalFats(round(sum(items, AnalyzedFoodResponse::getFats)))
                .totalFibers(round(sum(items, AnalyzedFoodResponse::getFibers)))
                .totalSugars(round(sum(items, AnalyzedFoodResponse::getSugars)))
                .totalSaturatedFat(round(sum(items, AnalyzedFoodResponse::getSaturatedFat)))
                .totalSalt(round(sum(items, AnalyzedFoodResponse::getSalt)))
                .micros(micros)
                .microCoverage(kcal > 0 ? round(kcalWithMicros / kcal) : 0.0)
                .build();
    }

    // ---- Lecture de la réponse du modèle ----

    private DetectedMeal readAnalysis(String reply) {
        JsonNode root = parseJson(reply);
        JsonNode itemsNode = root.isArray() ? root : root.path("items");

        List<DetectedFood> items = new ArrayList<>();
        for (JsonNode node : itemsNode) {
            String name = node.path("name").asText("").trim();
            double grams = node.path("quantityGrams").asDouble(0);
            if (!name.isEmpty() && grams > 0 && items.size() < MAX_ITEMS) {
                items.add(new DetectedFood(name, grams));
            }
        }
        if (items.isEmpty()) {
            throw new ExternalApiException(
                    "Aucun aliment n'a pu être identifié. Réessaie avec une photo plus nette ou décris le repas.");
        }
        String mealName = root.path("mealName").asText("Repas");
        return new DetectedMeal(mealName.isBlank() ? "Repas" : mealName, items);
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
            JsonNode salvaged = salvage(text, '{', '}');
            if (salvaged == null) {
                salvaged = salvage(text, '[', ']');
            }
            if (salvaged != null) {
                return salvaged;
            }
            log.warn("Réponse IA non parsable en JSON : {}", reply);
            throw new ExternalApiException("L'IA a renvoyé une analyse illisible. Réessaie.");
        }
    }

    private JsonNode salvage(String text, char open, char close) {
        int start = text.indexOf(open);
        int end = text.lastIndexOf(close);
        if (start < 0 || end <= start) {
            return null;
        }
        try {
            return objectMapper.readTree(text.substring(start, end + 1));
        } catch (Exception ignored) {
            return null;
        }
    }

    private JsonNode extractArray(JsonNode root) {
        return root.isArray() ? root : root.path("items");
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

    // ---- Utilitaires ----

    private double sum(List<AnalyzedFoodResponse> items,
                       java.util.function.Function<AnalyzedFoodResponse, Double> field) {
        return items.stream().mapToDouble(item -> orZero(field.apply(item))).sum();
    }

    private Double scaleNullable(Double per100g, double ratio) {
        return per100g == null ? null : round(per100g * ratio);
    }

    private double positive(JsonNode node, String field) {
        return Math.max(0, node.path(field).asDouble(0));
    }

    private double orZero(Double value) {
        return value != null ? value : 0.0;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private record DetectedFood(String name, double quantityGrams) {
    }

    private record DetectedMeal(String mealName, List<DetectedFood> items) {
    }
}
