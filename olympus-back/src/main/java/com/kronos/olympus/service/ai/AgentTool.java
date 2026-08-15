package com.kronos.olympus.service.ai;

import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.function.Function;

/**
 * Un outil (function calling) que l'agent IA peut invoquer. L'exécuteur est lié à
 * l'utilisateur authentifié au moment de la construction : le LLM ne fournit jamais
 * d'identifiant utilisateur, ce qui rend toute action inter-utilisateurs impossible.
 */
public class AgentTool {

    private final String name;
    private final String description;
    private final Map<String, Object> parameters; // schéma JSON (type=object)
    private final Function<JsonNode, String> executor;

    public AgentTool(String name, String description, Map<String, Object> parameters,
                     Function<JsonNode, String> executor) {
        this.name = name;
        this.description = description;
        this.parameters = parameters;
        this.executor = executor;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    /** Exécute l'outil ; toute exception est convertie en message lisible pour le modèle. */
    public String execute(JsonNode args) {
        try {
            return executor.apply(args);
        } catch (Exception e) {
            return "Erreur lors de l'exécution de l'outil '" + name + "' : " + e.getMessage();
        }
    }
}
