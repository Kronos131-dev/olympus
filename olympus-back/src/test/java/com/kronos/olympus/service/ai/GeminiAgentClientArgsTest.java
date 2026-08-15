package com.kronos.olympus.service.ai;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Vérifie le durcissement du parsing des arguments Gemini (Fix 4) : quand l'API renvoie un
 * appel d'outil SANS champ {@code args} (cas des outils sans paramètre), le client doit fournir
 * un objet JSON vide à l'outil — et non un {@code MissingNode} susceptible de casser le parsing.
 */
@ExtendWith(MockitoExtension.class)
class GeminiAgentClientArgsTest {

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private LlmRateLimiter llmRateLimiter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void run_fournitUnObjetVideQuandGeminiOmetLesArgs() throws Exception {
        GeminiAgentClient client = new GeminiAgentClient(restTemplate, objectMapper, llmRateLimiter);
        // @Value non injectés hors Spring : on les fixe explicitement.
        ReflectionTestUtils.setField(client, "apiKey", "test-key");
        ReflectionTestUtils.setField(client, "model", "gemini-test");
        ReflectionTestUtils.setField(client, "maxToolRounds", 3);

        // Le limiteur de débit se contente d'exécuter l'appel fourni.
        when(llmRateLimiter.execute(eq("gemini"), any()))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(1)).get());

        // 1er tour : appel d'outil SANS "args". 2e tour : réponse textuelle de clôture.
        JsonNode round1 = objectMapper.readTree(
                "{\"candidates\":[{\"content\":{\"parts\":[{\"functionCall\":{\"name\":\"noargs_tool\"}}]}}]}");
        JsonNode round2 = objectMapper.readTree(
                "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Terminé\"}]}}]}");
        when(restTemplate.postForEntity(anyString(), any(), eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(round1))
                .thenReturn(ResponseEntity.ok(round2));

        // Outil qui capture les arguments reçus.
        AtomicReference<JsonNode> captured = new AtomicReference<>();
        AgentTool tool = new AgentTool(
                "noargs_tool", "Outil sans paramètre",
                Map.of("type", "object", "properties", Map.of()),
                args -> {
                    captured.set(args);
                    return "ok";
                });

        AgentResult result = client.run("system", List.of(), "vas-y", null, null, List.of(tool));

        JsonNode args = captured.get();
        assertNotNull(args, "L'outil aurait dû être invoqué");
        assertFalse(args.isMissingNode(), "args ne doit pas être un MissingNode");
        assertTrue(args.isObject(), "args doit être un objet JSON (vide)");
        assertEquals("Terminé", result.getReply());
    }
}
