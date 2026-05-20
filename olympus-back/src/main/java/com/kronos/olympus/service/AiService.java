package com.kronos.olympus.service;

import com.kronos.olympus.dto.response.AiEstimation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel; // Nouveau nom de l'interface
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter; // Nouveau nom du parseur
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    // On utilise ChatModel à la place de ChatClient
    private final ChatModel chatModel;

    public AiEstimation analyzeMeal(String description) {

        // On utilise BeanOutputConverter
        BeanOutputConverter<AiEstimation> outputConverter = new BeanOutputConverter<>(AiEstimation.class);

        String promptText = """
                Tu es un expert en nutrition. Un utilisateur va te décrire un repas qu'il a mangé.
                Ton rôle est d'estimer les valeurs nutritionnelles TOTALES de ce repas et le poids TOTAL en grammes.
                
                Si l'utilisateur ne précise pas les quantités, estime le poids total standard.
                S'il précise les poids ou quantités, calcule le total exact.
                
                Donne un nom générique court et clair à ce repas.
                
                Repas décrit par l'utilisateur : {description}
                
                {format}
                """;

        PromptTemplate promptTemplate = new PromptTemplate(promptText);

        Prompt prompt = promptTemplate.create(Map.of(
                "description", description,
                "format", outputConverter.getFormat()
        ));

        log.info("Appel de Mistral AI pour l'analyse du repas: {}", description);

        try {
            // L'appel se fait maintenant sur chatModel
            String aiResponse = chatModel.call(prompt).getResult().getOutput().getText();
            log.debug("Réponse brute de Mistral: {}", aiResponse);

            // On utilise .convert() au lieu de .parse()
            return outputConverter.convert(aiResponse);

        } catch (Exception e) {
            log.error("Erreur lors de l'analyse du repas par Mistral", e);
            throw new RuntimeException("Impossible d'analyser ce repas avec l'IA. Veuillez réessayer.");
        }
    }
}