package com.kronos.olympus.service;

import com.kronos.olympus.dto.response.AiEstimation;
import com.kronos.olympus.service.ai.LlmRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Filet de sécurité autour du seul consommateur de Spring AI du projet.
 *
 * <p>La montée en Spring AI 2.x, requise par Spring Boot 4, renomme l'artefact du starter et
 * réorganise l'auto-configuration. Ces tests fixent le contrat observable de la classe : le
 * prompt part avec la description de l'utilisateur et les consignes de format, la réponse JSON
 * revient désérialisée, et l'appel passe par le limiteur de débit partagé.
 */
@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private LlmRateLimiter llmRateLimiter;

    @InjectMocks
    private AiService aiService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void passThroughTheRateLimiter() {
        when(llmRateLimiter.execute(eq("mistral"), any(Supplier.class)))
                .thenAnswer(invocation -> ((Supplier<String>) invocation.getArgument(1)).get());
    }

    private void givenModelAnswers(String rawJson) {
        ChatResponse response = new ChatResponse(List.of(new Generation(new AssistantMessage(rawJson))));
        when(chatModel.call(any(Prompt.class))).thenReturn(response);
    }

    @Test
    void analyzeMeal_deserialisesTheModelAnswerIntoAnEstimation() {
        givenModelAnswers("""
                {
                  "name": "Bol de riz au poulet",
                  "totalWeightGrams": 450.0,
                  "totalKcal": 620.0,
                  "totalProteins": 48.0,
                  "totalCarbs": 70.0,
                  "totalFats": 12.0
                }
                """);

        AiEstimation estimation = aiService.analyzeMeal("un bol de riz avec du poulet");

        assertEquals("Bol de riz au poulet", estimation.getName());
        assertEquals(450.0, estimation.getTotalWeightGrams());
        assertEquals(620.0, estimation.getTotalKcal());
        assertEquals(48.0, estimation.getTotalProteins());
        assertEquals(70.0, estimation.getTotalCarbs());
        assertEquals(12.0, estimation.getTotalFats());
    }

    @Test
    void analyzeMeal_sendsTheUserDescriptionAndTheFormatInstructions() {
        givenModelAnswers("{\"name\":\"Omelette\"}");

        aiService.analyzeMeal("deux oeufs brouillés");

        ArgumentCaptor<Prompt> sent = ArgumentCaptor.forClass(Prompt.class);
        org.mockito.Mockito.verify(chatModel).call(sent.capture());

        String prompt = sent.getValue().getContents();
        assertTrue(prompt.contains("deux oeufs brouillés"), "la description doit figurer dans le prompt");
        assertTrue(prompt.toLowerCase().contains("json"),
                "les consignes de format du BeanOutputConverter doivent figurer dans le prompt");
    }

    @Test
    void analyzeMeal_whenTheModelFails_reportsAUsableError() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new IllegalStateException("Mistral indisponible"));

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> aiService.analyzeMeal("une pomme"));

        assertTrue(thrown.getMessage().contains("Impossible d'analyser ce repas"));
    }
}
