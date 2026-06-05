package com.kronos.olympus.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.kronos.olympus.dto.request.LogEntryRequest;
import com.kronos.olympus.dto.request.MealIngredientRequest;
import com.kronos.olympus.dto.request.MealPresetRequest;
import com.kronos.olympus.dto.response.DailyLogResponse;
import com.kronos.olympus.dto.response.FoodItemResponse;
import com.kronos.olympus.dto.response.LogEntryResponse;
import com.kronos.olympus.dto.response.MealPlanResponse;
import com.kronos.olympus.dto.response.MealPresetResponse;
import com.kronos.olympus.dto.response.PlannedMealEntryResponse;
import com.kronos.olympus.dto.response.UserResponse;
import com.kronos.olympus.mapper.UserMapper;
import com.kronos.olympus.model.FoodItem;
import com.kronos.olympus.model.User;
import com.kronos.olympus.model.enums.FoodSource;
import com.kronos.olympus.repository.FoodItemRepository;
import com.kronos.olympus.service.DailyLogService;
import com.kronos.olympus.service.FoodItemService;
import com.kronos.olympus.service.MealPresetService;
import com.kronos.olympus.service.mealplan.MealPlanGenerationService;
import com.kronos.olympus.service.mealplan.MealPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Construit l'ensemble des outils (function calling) que l'agent IA peut utiliser.
 * Chaque outil est lié à l'utilisateur authentifié passé à {@link #buildTools(User)} :
 * aucun identifiant utilisateur ne transite par le LLM.
 */
@Component
@RequiredArgsConstructor
public class NutritionAgentTools {

    private final DailyLogService dailyLogService;
    private final MealPresetService mealPresetService;
    private final FoodItemService foodItemService;
    private final FoodItemRepository foodItemRepository;
    private final MealPlanGenerationService mealPlanGenerationService;
    private final MealPlanService mealPlanService;
    private final UserMapper userMapper;

    /**
     * Recherche un aliment en privilégiant la base CIQUAL (officielle), puis Open Food Facts
     * en secours. Renvoie une liste vide si rien n'est trouvé.
     */
    private List<FoodItemResponse> searchCiqualFirst(String query) {
        List<FoodItemResponse> results = foodItemService.searchCiqualFoods(query);
        if (results.isEmpty()) {
            results = foodItemService.searchFoodItemsByName(query);
        }
        return results;
    }

    public List<AgentTool> buildTools(User user) {
        List<AgentTool> tools = new ArrayList<>();

        tools.add(new AgentTool(
                "get_user_profile",
                "Récupère le profil de l'utilisateur et ses objectifs nutritionnels journaliers (calories et macros cibles).",
                objectSchema(new LinkedHashMap<>(), List.of()),
                args -> {
                    UserResponse p = userMapper.toResponse(user);
                    return "Profil : objectif=" + p.getGoal() + ", activité=" + p.getActivityLevel()
                            + ", poids=" + p.getCurrentWeightKg() + "kg, taille=" + p.getHeightCm() + "cm.\n"
                            + "Cibles journalières : " + intOf(p.getTargetKcal()) + " kcal, "
                            + intOf(p.getTargetProteins()) + "g protéines, "
                            + intOf(p.getTargetCarbs()) + "g glucides, "
                            + intOf(p.getTargetFats()) + "g lipides.";
                }));

        tools.add(new AgentTool(
                "get_daily_log",
                "Récupère le journal alimentaire de l'utilisateur pour une date donnée (consommation et entrées).",
                objectSchema(props(
                        "date", strProp("Date ISO yyyy-MM-dd. Par défaut : aujourd'hui.")
                ), List.of()),
                args -> {
                    LocalDate date = resolveDate(args, "date");
                    DailyLogResponse log = dailyLogService.getDailyLogByDate(user, date);
                    StringBuilder sb = new StringBuilder();
                    sb.append("Journal du ").append(date).append(" : ")
                            .append(intOf(log.getTotalKcal())).append(" kcal, ")
                            .append(intOf(log.getTotalProteins())).append("g P, ")
                            .append(intOf(log.getTotalCarbs())).append("g G, ")
                            .append(intOf(log.getTotalFats())).append("g L.\n");
                    if (log.getEntries() == null || log.getEntries().isEmpty()) {
                        sb.append("Aucune entrée.");
                    } else {
                        sb.append("Entrées :\n");
                        for (LogEntryResponse e : log.getEntries()) {
                            String name = e.getFoodItem() != null ? e.getFoodItem().getName()
                                    : (e.getMealPreset() != null ? e.getMealPreset().getName() : "?");
                            sb.append("- [entryId=").append(e.getId()).append("] ").append(name);
                            if (e.getFoodItem() != null && e.getQuantityGrams() != null) {
                                sb.append(" (").append(intOf(e.getQuantityGrams())).append("g)");
                            }
                            sb.append("\n");
                        }
                    }
                    return sb.toString();
                }));

        tools.add(new AgentTool(
                "get_meal_presets",
                "Liste les repas pré-enregistrés de l'utilisateur avec leur identifiant et leurs macros.",
                objectSchema(new LinkedHashMap<>(), List.of()),
                args -> {
                    List<MealPresetResponse> presets = mealPresetService.getUserMealPresets(user.getId());
                    if (presets.isEmpty()) {
                        return "Aucun repas pré-enregistré.";
                    }
                    StringBuilder sb = new StringBuilder("Repas pré-enregistrés :\n");
                    for (MealPresetResponse m : presets) {
                        sb.append("- [mealPresetId=").append(m.getId()).append("] ").append(m.getName())
                                .append(" (").append(intOf(m.getTotalKcal())).append(" kcal)\n");
                    }
                    return sb.toString();
                }));

        tools.add(new AgentTool(
                "get_meal_plan",
                "Récupère l'emploi du temps hebdomadaire des repas planifiés de l'utilisateur, "
                        + "jour par jour, avec les valeurs caloriques estimées. Utile pour consulter ou analyser la diète prévue sur la semaine.",
                objectSchema(new LinkedHashMap<>(), List.of()),
                args -> {
                    MealPlanResponse plan = mealPlanService.getWeeklyPlan(user);
                    List<PlannedMealEntryResponse> entries = plan.getEntries();
                    if (entries == null || entries.isEmpty()) {
                        return "Aucun repas planifié dans l'emploi du temps hebdomadaire.";
                    }
                    StringBuilder sb = new StringBuilder("Planning hebdomadaire des repas :\n");
                    for (DayOfWeek day : DayOfWeek.values()) {
                        List<PlannedMealEntryResponse> dayEntries = new ArrayList<>();
                        for (PlannedMealEntryResponse e : entries) {
                            if (e.getDayOfWeek() == day) {
                                dayEntries.add(e);
                            }
                        }
                        if (dayEntries.isEmpty()) {
                            continue;
                        }
                        sb.append(dayLabelFr(day)).append(" :\n");
                        double dayKcal = 0;
                        for (PlannedMealEntryResponse e : dayEntries) {
                            double qty = e.getQuantityGrams() != null ? e.getQuantityGrams() : 0;
                            if (e.getFoodItem() != null) {
                                FoodItemResponse f = e.getFoodItem();
                                double kcal = f.getKcal100g() != null ? f.getKcal100g() * qty / 100.0 : 0;
                                dayKcal += kcal;
                                sb.append("  - ").append(f.getName()).append(" (").append(intOf(qty))
                                        .append("g, ").append(intOf(kcal)).append(" kcal)\n");
                            } else if (e.getMealPreset() != null) {
                                MealPresetResponse m = e.getMealPreset();
                                double kcal = m.getTotalKcal() != null ? m.getTotalKcal() : 0;
                                dayKcal += kcal;
                                sb.append("  - ").append(m.getName()).append(" (repas pré-enregistré, ")
                                        .append(intOf(kcal)).append(" kcal)\n");
                            }
                        }
                        sb.append("  Total ").append(dayLabelFr(day)).append(" : ")
                                .append(intOf(dayKcal)).append(" kcal\n");
                    }
                    return sb.toString();
                }));

        tools.add(new AgentTool(
                "search_food_items",
                "Recherche des aliments par nom, en priorité dans la base officielle CIQUAL "
                        + "puis dans Open Food Facts. Renvoie les identifiants et valeurs pour 100g.",
                objectSchema(props(
                        "query", strProp("Nom de l'aliment à rechercher.")
                ), List.of("query")),
                args -> {
                    String query = args.path("query").asText("");
                    List<FoodItemResponse> results = searchCiqualFirst(query);
                    if (results.isEmpty()) {
                        return "Aucun aliment trouvé pour '" + query + "' en base. "
                                + "Tu peux estimer ses valeurs et utiliser log_estimated_food.";
                    }
                    StringBuilder sb = new StringBuilder("Résultats pour '" + query + "' :\n");
                    int limit = Math.min(results.size(), 10);
                    for (int i = 0; i < limit; i++) {
                        FoodItemResponse f = results.get(i);
                        sb.append("- [foodItemId=").append(f.getId()).append("] ").append(f.getName())
                                .append(" — ").append(intOf(f.getKcal100g())).append(" kcal/100g\n");
                    }
                    return sb.toString();
                }));

        tools.add(new AgentTool(
                "log_food",
                "Ajoute un aliment au journal de l'utilisateur. Recherche l'aliment par son nom "
                        + "en priorité dans la base CIQUAL, puis Open Food Facts, et enregistre la quantité. "
                        + "Si l'aliment est introuvable, utilise plutôt log_estimated_food.",
                objectSchema(props(
                        "foodName", strProp("Nom de l'aliment à ajouter."),
                        "quantityGrams", numProp("Quantité consommée en grammes."),
                        "date", strProp("Date ISO yyyy-MM-dd. Par défaut : aujourd'hui.")
                ), List.of("foodName", "quantityGrams")),
                args -> {
                    String foodName = args.path("foodName").asText("");
                    double qty = args.path("quantityGrams").asDouble(0);
                    if (foodName.isBlank() || qty <= 0) {
                        return "Le nom de l'aliment et une quantité positive sont requis.";
                    }
                    LocalDate date = resolveDate(args, "date");

                    List<FoodItemResponse> results = searchCiqualFirst(foodName);
                    if (results.isEmpty()) {
                        return "Aucun aliment trouvé pour '" + foodName + "' en base CIQUAL/OFF. "
                                + "Estime ses valeurs nutritionnelles pour 100g et utilise log_estimated_food.";
                    }
                    FoodItemResponse food = results.get(0);
                    LogEntryRequest req = LogEntryRequest.builder()
                            .targetDate(date)
                            .foodItemId(food.getId())
                            .quantityGrams(qty)
                            .build();
                    DailyLogResponse log = dailyLogService.addLogEntry(user, req);
                    return "Ajouté : " + intOf(qty) + "g de '" + food.getName() + "' le " + date
                            + ". Total du jour : " + intOf(log.getTotalKcal()) + " kcal.";
                }));

        tools.add(new AgentTool(
                "log_estimated_food",
                "Ajoute au journal un aliment ABSENT de la base de données, en utilisant des valeurs "
                        + "nutritionnelles que TU estimes (pour 100g). À n'utiliser qu'après un échec de "
                        + "recherche via log_food ou search_food_items.",
                objectSchema(props(
                        "name", strProp("Nom de l'aliment."),
                        "quantityGrams", numProp("Quantité consommée en grammes."),
                        "kcalPer100g", numProp("Calories estimées pour 100g."),
                        "proteinsPer100g", numProp("Protéines estimées en grammes pour 100g."),
                        "carbsPer100g", numProp("Glucides estimés en grammes pour 100g."),
                        "fatsPer100g", numProp("Lipides estimés en grammes pour 100g."),
                        "date", strProp("Date ISO yyyy-MM-dd. Par défaut : aujourd'hui.")
                ), List.of("name", "quantityGrams", "kcalPer100g", "proteinsPer100g", "carbsPer100g", "fatsPer100g")),
                args -> {
                    String name = args.path("name").asText("");
                    double qty = args.path("quantityGrams").asDouble(0);
                    if (name.isBlank() || qty <= 0) {
                        return "Le nom de l'aliment et une quantité positive sont requis.";
                    }
                    LocalDate date = resolveDate(args, "date");

                    FoodItem food = FoodItem.builder()
                            .name("IA : " + name)
                            .kcal100g(Math.max(0, args.path("kcalPer100g").asDouble(0)))
                            .proteins100g(Math.max(0, args.path("proteinsPer100g").asDouble(0)))
                            .carbs100g(Math.max(0, args.path("carbsPer100g").asDouble(0)))
                            .fats100g(Math.max(0, args.path("fatsPer100g").asDouble(0)))
                            .source(FoodSource.AI)
                            .barcode("AI-" + UUID.randomUUID().toString().substring(0, 8))
                            .build();
                    FoodItem saved = foodItemRepository.save(food);

                    LogEntryRequest req = LogEntryRequest.builder()
                            .targetDate(date)
                            .foodItemId(saved.getId())
                            .quantityGrams(qty)
                            .build();
                    DailyLogResponse log = dailyLogService.addLogEntry(user, req);
                    return "Ajouté (estimation IA) : " + intOf(qty) + "g de '" + name + "' le " + date
                            + ". Total du jour : " + intOf(log.getTotalKcal()) + " kcal.";
                }));

        tools.add(new AgentTool(
                "log_meal_preset",
                "Ajoute un repas pré-enregistré (par son identifiant) au journal de l'utilisateur.",
                objectSchema(props(
                        "mealPresetId", numProp("Identifiant du repas pré-enregistré."),
                        "date", strProp("Date ISO yyyy-MM-dd. Par défaut : aujourd'hui.")
                ), List.of("mealPresetId")),
                args -> {
                    long presetId = args.path("mealPresetId").asLong(0);
                    if (presetId <= 0) {
                        return "Un identifiant de repas valide est requis. Utilise get_meal_presets.";
                    }
                    LocalDate date = resolveDate(args, "date");
                    LogEntryRequest req = LogEntryRequest.builder()
                            .targetDate(date)
                            .mealPresetId(presetId)
                            .build();
                    DailyLogResponse log = dailyLogService.addLogEntry(user, req);
                    return "Repas ajouté au journal du " + date
                            + ". Total du jour : " + intOf(log.getTotalKcal()) + " kcal.";
                }));

        tools.add(new AgentTool(
                "create_meal_preset",
                "Crée un repas pré-enregistré composé de plusieurs aliments. Chaque ingrédient est recherché par son nom.",
                objectSchema(props(
                        "name", strProp("Nom du repas."),
                        "ingredients", arrayOfIngredients()
                ), List.of("name", "ingredients")),
                args -> {
                    String name = args.path("name").asText("");
                    JsonNode ingredients = args.path("ingredients");
                    if (name.isBlank() || !ingredients.isArray() || ingredients.isEmpty()) {
                        return "Un nom et au moins un ingrédient sont requis.";
                    }
                    List<MealIngredientRequest> ingReqs = new ArrayList<>();
                    List<String> notFound = new ArrayList<>();
                    for (JsonNode ing : ingredients) {
                        String ingName = ing.path("foodName").asText("");
                        double q = ing.path("quantityGrams").asDouble(0);
                        if (ingName.isBlank() || q <= 0) {
                            continue;
                        }
                        List<FoodItemResponse> found = searchCiqualFirst(ingName);
                        if (found.isEmpty()) {
                            notFound.add(ingName);
                            continue;
                        }
                        ingReqs.add(MealIngredientRequest.builder()
                                .foodItemId(found.get(0).getId())
                                .quantityGrams(q)
                                .build());
                    }
                    if (ingReqs.isEmpty()) {
                        return "Aucun ingrédient n'a pu être identifié : " + String.join(", ", notFound);
                    }
                    MealPresetRequest req = MealPresetRequest.builder()
                            .name(name)
                            .ingredients(ingReqs)
                            .build();
                    MealPresetResponse created = mealPresetService.createMealPreset(user, req);
                    String msg = "Repas '" + created.getName() + "' créé (mealPresetId=" + created.getId()
                            + ", " + intOf(created.getTotalKcal()) + " kcal).";
                    if (!notFound.isEmpty()) {
                        msg += " Ingrédients non trouvés et ignorés : " + String.join(", ", notFound) + ".";
                    }
                    return msg;
                }));

        tools.add(new AgentTool(
                "create_meal_plan",
                "Crée ou remplace l'emploi du temps hebdomadaire de repas de l'utilisateur. "
                        + "Fournis pour chaque repas le jour de la semaine et les valeurs nutritionnelles "
                        + "estimées pour 100g.",
                objectSchema(props(
                        "meals", arrayOfPlannedMeals()
                ), List.of("meals")),
                args -> {
                    JsonNode meals = args.path("meals");
                    if (!meals.isArray() || meals.isEmpty()) {
                        return "Fournis un tableau 'meals' non vide.";
                    }
                    MealPlanResponse plan = mealPlanGenerationService.applyGeneratedPlan(user, meals);
                    int count = plan.getEntries() != null ? plan.getEntries().size() : 0;
                    return "Emploi du temps hebdomadaire enregistré : " + count
                            + " repas planifiés sur la semaine.";
                }));

        tools.add(new AgentTool(
                "delete_meal_preset",
                "Supprime un repas pré-enregistré de l'utilisateur par son identifiant.",
                objectSchema(props(
                        "mealPresetId", numProp("Identifiant du repas pré-enregistré à supprimer.")
                ), List.of("mealPresetId")),
                args -> {
                    long presetId = args.path("mealPresetId").asLong(0);
                    if (presetId <= 0) {
                        return "Un identifiant de repas valide est requis.";
                    }
                    mealPresetService.deleteMealPreset(presetId, user.getId());
                    return "Repas pré-enregistré supprimé.";
                }));

        tools.add(new AgentTool(
                "delete_log_entry",
                "Supprime une entrée du journal alimentaire de l'utilisateur par son identifiant (entryId).",
                objectSchema(props(
                        "entryId", numProp("Identifiant de l'entrée de journal à supprimer.")
                ), List.of("entryId")),
                args -> {
                    long entryId = args.path("entryId").asLong(0);
                    if (entryId <= 0) {
                        return "Un identifiant d'entrée valide est requis. Utilise get_daily_log.";
                    }
                    DailyLogResponse log = dailyLogService.removeLogEntry(user, entryId);
                    return "Entrée supprimée. Total du jour : " + intOf(log.getTotalKcal()) + " kcal.";
                }));

        return tools;
    }

    // --- Helpers de schéma JSON ---

    private static Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    private static Map<String, Object> props(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        return map;
    }

    private static Map<String, Object> strProp(String description) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("type", "string");
        p.put("description", description);
        return p;
    }

    private static Map<String, Object> numProp(String description) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("type", "number");
        p.put("description", description);
        return p;
    }

    private static Map<String, Object> arrayOfIngredients() {
        Map<String, Object> itemProps = new LinkedHashMap<>();
        itemProps.put("foodName", strProp("Nom de l'aliment ingrédient."));
        itemProps.put("quantityGrams", numProp("Quantité en grammes."));

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", "object");
        item.put("properties", itemProps);
        item.put("required", List.of("foodName", "quantityGrams"));

        Map<String, Object> array = new LinkedHashMap<>();
        array.put("type", "array");
        array.put("description", "Liste des ingrédients du repas.");
        array.put("items", item);
        return array;
    }

    private static Map<String, Object> arrayOfPlannedMeals() {
        Map<String, Object> itemProps = new LinkedHashMap<>();
        itemProps.put("day", strProp("Jour de la semaine en anglais majuscules : MONDAY, TUESDAY, "
                + "WEDNESDAY, THURSDAY, FRIDAY, SATURDAY ou SUNDAY."));
        itemProps.put("name", strProp("Nom du plat ou de l'aliment."));
        itemProps.put("quantityGrams", numProp("Quantité en grammes."));
        itemProps.put("kcalPer100g", numProp("Calories pour 100g."));
        itemProps.put("proteinsPer100g", numProp("Protéines en grammes pour 100g."));
        itemProps.put("carbsPer100g", numProp("Glucides en grammes pour 100g."));
        itemProps.put("fatsPer100g", numProp("Lipides en grammes pour 100g."));

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", "object");
        item.put("properties", itemProps);
        item.put("required", List.of("day", "name", "quantityGrams",
                "kcalPer100g", "proteinsPer100g", "carbsPer100g", "fatsPer100g"));

        Map<String, Object> array = new LinkedHashMap<>();
        array.put("type", "array");
        array.put("description", "Liste des repas planifiés sur la semaine.");
        array.put("items", item);
        return array;
    }

    private static LocalDate resolveDate(JsonNode args, String field) {
        JsonNode node = args.path(field);
        if (node.isMissingNode() || node.isNull() || node.asText("").isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(node.asText().trim());
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private static String intOf(Double value) {
        return value == null ? "0" : String.valueOf((int) Math.round(value));
    }

    private static String intOf(double value) {
        return String.valueOf((int) Math.round(value));
    }

    private static String dayLabelFr(DayOfWeek day) {
        switch (day) {
            case MONDAY: return "Lundi";
            case TUESDAY: return "Mardi";
            case WEDNESDAY: return "Mercredi";
            case THURSDAY: return "Jeudi";
            case FRIDAY: return "Vendredi";
            case SATURDAY: return "Samedi";
            case SUNDAY: return "Dimanche";
            default: return day.name();
        }
    }
}
