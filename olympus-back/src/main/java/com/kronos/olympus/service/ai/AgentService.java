package com.kronos.olympus.service.ai;

import com.kronos.olympus.dto.response.ChatMessageDto;
import com.kronos.olympus.dto.response.ChatResponseDto;
import com.kronos.olympus.dto.response.ConversationSummaryDto;
import com.kronos.olympus.exception.EntityNotFoundException;
import com.kronos.olympus.exception.ExternalApiException;
import com.kronos.olympus.model.ChatMessage;
import com.kronos.olympus.model.Conversation;
import com.kronos.olympus.model.User;
import com.kronos.olympus.model.enums.AiProvider;
import com.kronos.olympus.model.enums.ChatRole;
import com.kronos.olympus.repository.ChatMessageRepository;
import com.kronos.olympus.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Orchestration de l'agent IA conversationnel : sélection du fournisseur, exécution
 * du tour avec outils, et persistance de la conversation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentService {

    // Fenêtre d'historique envoyée au LLM : volontairement limitée pour réduire la
    // taille des prompts (donc les tokens et le risque de dépassement de débit).
    private static final int MAX_HISTORY = 12;

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final NutritionAgentTools nutritionAgentTools;
    private final MistralAgentClient mistralAgentClient;
    private final GeminiAgentClient geminiAgentClient;

    @Transactional
    public ChatResponseDto chat(User user, Long conversationId, String message, MultipartFile image) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Le message ne peut pas être vide.");
        }

        Conversation conversation = resolveConversation(user, conversationId, message);

        // Historique texte (sans le message courant, pas encore persisté)
        List<ChatTurn> history = loadHistory(conversation.getId());

        // Image éventuelle
        String imageBase64 = null;
        String imageMime = null;
        boolean hasImage = image != null && !image.isEmpty();
        if (hasImage) {
            try {
                imageBase64 = Base64.getEncoder().encodeToString(image.getBytes());
                imageMime = image.getContentType() != null ? image.getContentType() : "image/jpeg";
            } catch (IOException e) {
                throw new IllegalArgumentException("Impossible de lire l'image envoyée.");
            }
        }

        AgentClient client = selectClient(user, hasImage);
        String providerName = (client == geminiAgentClient) ? "GEMINI" : "MISTRAL";

        List<AgentTool> tools = nutritionAgentTools.buildTools(user);
        AgentResult result = client.run(buildSystemPrompt(user), history, message,
                imageBase64, imageMime, tools);

        // Persistance du tour
        persistMessage(conversation, ChatRole.USER, message);
        persistMessage(conversation, ChatRole.ASSISTANT, result.getReply());
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        return ChatResponseDto.builder()
                .conversationId(conversation.getId())
                .reply(result.getReply())
                .actionsTaken(result.getActionsTaken())
                .provider(providerName)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ConversationSummaryDto> getConversations(Long userId) {
        return conversationRepository.findAllByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(c -> ConversationSummaryDto.builder()
                        .id(c.getId())
                        .title(c.getTitle())
                        .updatedAt(c.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getConversationMessages(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation introuvable : " + conversationId));
        return chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId()).stream()
                .map(m -> ChatMessageDto.builder()
                        .id(m.getId())
                        .role(m.getRole())
                        .content(m.getContent())
                        .createdAt(m.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteConversation(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation introuvable : " + conversationId));
        conversationRepository.delete(conversation);
    }

    private Conversation resolveConversation(User user, Long conversationId, String firstMessage) {
        if (conversationId != null) {
            return conversationRepository.findByIdAndUserId(conversationId, user.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Conversation introuvable : " + conversationId));
        }
        String title = firstMessage.length() > 60 ? firstMessage.substring(0, 60) + "…" : firstMessage;
        return conversationRepository.save(Conversation.builder()
                .user(user)
                .title(title)
                .build());
    }

    private List<ChatTurn> loadHistory(Long conversationId) {
        List<ChatMessage> persisted = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        List<ChatTurn> history = new ArrayList<>();
        int start = Math.max(0, persisted.size() - MAX_HISTORY);
        for (int i = start; i < persisted.size(); i++) {
            ChatMessage m = persisted.get(i);
            if (m.getRole() == ChatRole.USER) {
                history.add(new ChatTurn("user", m.getContent()));
            } else if (m.getRole() == ChatRole.ASSISTANT) {
                history.add(new ChatTurn("assistant", m.getContent()));
            }
        }
        return history;
    }

    private AgentClient selectClient(User user, boolean hasImage) {
        boolean wantsGemini = user.getAiProvider() == AiProvider.GEMINI;
        // Image jointe : Gemini est privilégié (multimodal fiable) s'il est configuré
        if (hasImage && geminiAgentClient.isConfigured()) {
            return geminiAgentClient;
        }
        if (wantsGemini && geminiAgentClient.isConfigured()) {
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

    private void persistMessage(Conversation conversation, ChatRole role, String content) {
        chatMessageRepository.save(ChatMessage.builder()
                .conversation(conversation)
                .role(role)
                .content(content)
                .build());
    }

    private String buildSystemPrompt(User user) {
        return "Tu es l'assistant nutritionnel intelligent de l'application Olympus.\n"
                + "Date du jour : " + LocalDate.now() + ".\n"
                + "Tu aides l'utilisateur à suivre son alimentation : analyser des plats, aliments et repas, "
                + "les ajouter à son journal journalier, créer ou supprimer des repas pré-enregistrés, "
                + "consulter ses quotas et son profil, et lui donner des conseils nutritionnels.\n"
                + "Utilise SYSTÉMATIQUEMENT les outils à ta disposition pour AGIR concrètement plutôt que "
                + "de seulement décrire ce que tu ferais.\n"
                + "Quand l'utilisateur décrit un repas ou envoie une photo, identifie chaque aliment et "
                + "ajoute-le au journal via l'outil log_food (en estimant les quantités si besoin).\n"
                + "SOURCE DES DONNÉES NUTRITIONNELLES : cherche TOUJOURS d'abord l'aliment en base via "
                + "log_food ou search_food_items — ces outils interrogent en priorité la base officielle "
                + "CIQUAL, puis Open Food Facts. Utilise log_estimated_food (en estimant toi-même les "
                + "valeurs pour 100g) UNIQUEMENT si l'aliment est introuvable en base.\n"
                + "Pour planifier les repas de la semaine, utilise l'outil create_meal_plan en "
                + "fournissant, pour chaque jour, les repas et leurs valeurs nutritionnelles estimées pour 100g.\n"
                + "Si une information essentielle manque, demande une précision à l'utilisateur.\n"
                + "Réponds toujours en français, de manière concise, claire et bienveillante. "
                + "N'utilise JAMAIS de formatage Markdown : pas d'astérisques (*), pas de dièses (#), "
                + "pas de tirets de liste — réponds en texte brut uniquement.";
    }
}
