package com.kronos.olympus.service.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kronos.olympus.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent IA via l'API Google Gemini (Generative Language API + function calling).
 * Utilise une simple clé d'API (pas de projet GCP / Vertex AI requis).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GeminiAgentClient implements AgentClient {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final LlmRateLimiter llmRateLimiter;

    @Value("${olympus.agent.gemini-api-key:}")
    private String apiKey;

    @Value("${olympus.agent.gemini-model:gemini-2.0-flash}")
    private String model;

    @Value("${olympus.agent.max-tool-rounds:3}")
    private int maxToolRounds;

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public AgentResult run(String systemPrompt, List<ChatTurn> history, String userText,
                           String imageBase64, String imageMimeType, List<AgentTool> tools) {

        Map<String, AgentTool> toolMap = new HashMap<>();
        List<Map<String, Object>> functionDeclarations = new ArrayList<>();
        for (AgentTool tool : tools) {
            toolMap.put(tool.getName(), tool);
            Map<String, Object> declaration = new LinkedHashMap<>();
            declaration.put("name", tool.getName());
            declaration.put("description", tool.getDescription());
            Object properties = tool.getParameters().get("properties");
            boolean hasProperties = properties instanceof Map && !((Map<?, ?>) properties).isEmpty();
            if (hasProperties) {
                declaration.put("parameters", geminiSchema(tool.getParameters()));
            }
            functionDeclarations.add(declaration);
        }

        List<Object> contents = new ArrayList<>();
        for (ChatTurn turn : history) {
            String role = "assistant".equals(turn.getRole()) ? "model" : "user";
            contents.add(content(role, List.of(textPart(turn.getContent()))));
        }
        List<Object> userParts = new ArrayList<>();
        userParts.add(textPart(userText));
        if (imageBase64 != null && !imageBase64.isBlank()) {
            Map<String, Object> inlineData = new LinkedHashMap<>();
            inlineData.put("mimeType", imageMimeType != null ? imageMimeType : "image/jpeg");
            inlineData.put("data", imageBase64);
            Map<String, Object> imagePart = new LinkedHashMap<>();
            imagePart.put("inlineData", inlineData);
            userParts.add(imagePart);
        }
        contents.add(content("user", userParts));

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;
        List<String> actionsTaken = new ArrayList<>();

        for (int round = 0; round < maxToolRounds; round++) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("systemInstruction", Map.of("parts", List.of(textPart(systemPrompt))));
            body.put("contents", contents);
            if (!functionDeclarations.isEmpty()) {
                body.put("tools", List.of(Map.of("functionDeclarations", functionDeclarations)));
            }

            JsonNode response = post(url, body);
            JsonNode candidateContent = response.path("candidates").path(0).path("content");
            if (candidateContent.isMissingNode()) {
                log.warn("Réponse inattendue de l'API Gemini : {}", response);
                throw new ExternalApiException("L'agent Gemini a renvoyé une réponse inattendue.");
            }
            JsonNode parts = candidateContent.path("parts");

            List<JsonNode> functionCalls = new ArrayList<>();
            StringBuilder text = new StringBuilder();
            if (parts.isArray()) {
                for (JsonNode part : parts) {
                    if (part.has("functionCall")) {
                        functionCalls.add(part.get("functionCall"));
                    } else if (part.has("text")) {
                        text.append(part.get("text").asText());
                    }
                }
            }

            if (!functionCalls.isEmpty()) {
                // On rejoue le contenu "model" tel quel
                contents.add(objectMapper.convertValue(candidateContent, MAP_TYPE));
                List<Object> responseParts = new ArrayList<>();
                for (JsonNode functionCall : functionCalls) {
                    String name = functionCall.path("name").asText("");
                    JsonNode args = functionCall.path("args");
                    AgentTool tool = toolMap.get(name);
                    String result = tool != null ? tool.execute(args) : "Outil inconnu : " + name;
                    actionsTaken.add(name);

                    Map<String, Object> functionResponse = new LinkedHashMap<>();
                    functionResponse.put("name", name);
                    functionResponse.put("response", Map.of("result", result));
                    Map<String, Object> responsePart = new LinkedHashMap<>();
                    responsePart.put("functionResponse", functionResponse);
                    responseParts.add(responsePart);
                }
                contents.add(content("user", responseParts));
                continue;
            }

            return new AgentResult(text.toString(), actionsTaken);
        }

        return new AgentResult("Je n'ai pas pu finaliser la demande (trop d'étapes d'outils).", actionsTaken);
    }

    private Map<String, Object> textPart(String text) {
        Map<String, Object> part = new LinkedHashMap<>();
        part.put("text", text);
        return part;
    }

    private Map<String, Object> content(String role, List<?> parts) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("role", role);
        content.put("parts", parts);
        return content;
    }

    /** Convertit un schéma JSON standard vers le sous-ensemble OpenAPI attendu par Gemini (type en MAJUSCULES). */
    @SuppressWarnings("unchecked")
    private Map<String, Object> geminiSchema(Map<String, Object> schema) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : schema.entrySet()) {
            Object value = entry.getValue();
            if ("type".equals(entry.getKey()) && value instanceof String) {
                out.put("type", ((String) value).toUpperCase());
            } else if (value instanceof Map) {
                out.put(entry.getKey(), geminiSchema((Map<String, Object>) value));
            } else {
                out.put(entry.getKey(), value);
            }
        }
        return out;
    }

    private JsonNode post(String url, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            // L'appel passe par le limiteur de débit : cadence respectée + retry sur 429/503.
            JsonNode response = llmRateLimiter.execute("gemini", () -> restTemplate
                    .postForEntity(url, new HttpEntity<>(body, headers), JsonNode.class)
                    .getBody());
            if (response == null) {
                throw new ExternalApiException("L'API Gemini a renvoyé une réponse vide.");
            }
            return response;
        } catch (RestClientException e) {
            log.error("Erreur lors de l'appel à l'API Gemini", e);
            throw new ExternalApiException("L'agent Gemini est momentanément indisponible.");
        }
    }
}
