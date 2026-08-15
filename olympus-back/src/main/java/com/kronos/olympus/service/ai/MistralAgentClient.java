package com.kronos.olympus.service.ai;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
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
 * Agent IA via l'API Mistral (compatible chat/completions + function calling).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MistralAgentClient implements AgentClient {

    private static final String API_URL = "https://api.mistral.ai/v1/chat/completions";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final LlmRateLimiter llmRateLimiter;

    @Value("${spring.ai.mistralai.api-key:}")
    private String apiKey;

    @Value("${olympus.agent.mistral-model:mistral-small-latest}")
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
        List<Map<String, Object>> toolSpecs = new ArrayList<>();
        for (AgentTool tool : tools) {
            toolMap.put(tool.getName(), tool);
            Map<String, Object> function = new LinkedHashMap<>();
            function.put("name", tool.getName());
            function.put("description", tool.getDescription());
            function.put("parameters", tool.getParameters());
            Map<String, Object> spec = new LinkedHashMap<>();
            spec.put("type", "function");
            spec.put("function", function);
            toolSpecs.add(spec);
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(textMessage("system", systemPrompt));
        for (ChatTurn turn : history) {
            messages.add(textMessage(turn.getRole(), turn.getContent()));
        }
        messages.add(buildUserMessage(userText, imageBase64, imageMimeType));

        List<String> actionsTaken = new ArrayList<>();

        for (int round = 0; round < maxToolRounds; round++) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("messages", messages);
            if (!toolSpecs.isEmpty()) {
                body.put("tools", toolSpecs);
                body.put("tool_choice", "auto");
            }
            body.put("temperature", 0.3);

            JsonNode response = post(body);
            JsonNode message = response.path("choices").path(0).path("message");
            if (message.isMissingNode()) {
                log.warn("Réponse inattendue de l'API Mistral : {}", response);
                throw new ExternalApiException("L'agent Mistral a renvoyé une réponse inattendue.");
            }
            JsonNode toolCalls = message.path("tool_calls");

            if (toolCalls.isArray() && !toolCalls.isEmpty()) {
                // On rejoue le message assistant tel quel (il porte les tool_calls)
                messages.add(objectMapper.convertValue(message, MAP_TYPE));
                for (JsonNode call : toolCalls) {
                    String id = call.path("id").asText("");
                    String name = call.path("function").path("name").asText("");
                    String argsStr = call.path("function").path("arguments").asText("{}");
                    AgentTool tool = toolMap.get(name);
                    String result = tool != null ? tool.execute(parseArgs(argsStr))
                            : "Outil inconnu : " + name;
                    actionsTaken.add(name);

                    Map<String, Object> toolMsg = new LinkedHashMap<>();
                    toolMsg.put("role", "tool");
                    toolMsg.put("name", name);
                    toolMsg.put("tool_call_id", id);
                    toolMsg.put("content", result);
                    messages.add(toolMsg);
                }
                continue;
            }

            return new AgentResult(message.path("content").asText(""), actionsTaken);
        }

        return new AgentResult("Je n'ai pas pu finaliser la demande (trop d'étapes d'outils).", actionsTaken);
    }

    private Map<String, Object> textMessage(String role, String content) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private Map<String, Object> buildUserMessage(String userText, String imageBase64, String imageMimeType) {
        if (imageBase64 == null || imageBase64.isBlank()) {
            return textMessage("user", userText);
        }
        String mime = imageMimeType != null ? imageMimeType : "image/jpeg";
        Map<String, Object> textPart = new LinkedHashMap<>();
        textPart.put("type", "text");
        textPart.put("text", userText);

        Map<String, Object> imageUrl = new LinkedHashMap<>();
        imageUrl.put("url", "data:" + mime + ";base64," + imageBase64);
        Map<String, Object> imagePart = new LinkedHashMap<>();
        imagePart.put("type", "image_url");
        imagePart.put("image_url", imageUrl);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", List.of(textPart, imagePart));
        return message;
    }

    private JsonNode parseArgs(String argsStr) {
        try {
            return objectMapper.readTree(argsStr);
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }

    private JsonNode post(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        try {
            // L'appel passe par le limiteur de débit : cadence respectée + retry sur 429/503.
            JsonNode response = llmRateLimiter.execute("mistral", () -> restTemplate
                    .postForEntity(API_URL, new HttpEntity<>(body, headers), JsonNode.class)
                    .getBody());
            if (response == null) {
                throw new ExternalApiException("L'API Mistral a renvoyé une réponse vide.");
            }
            return response;
        } catch (RestClientException e) {
            log.error("Erreur lors de l'appel à l'API Mistral", e);
            throw new ExternalApiException("L'agent Mistral est momentanément indisponible.");
        }
    }
}
