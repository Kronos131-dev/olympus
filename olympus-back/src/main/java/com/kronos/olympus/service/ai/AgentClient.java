package com.kronos.olympus.service.ai;

import java.util.List;

/**
 * Client d'agent IA capable de tenir une conversation avec function calling.
 * Implémenté pour chaque fournisseur (Mistral, Gemini).
 */
public interface AgentClient {

    /**
     * Exécute un tour de conversation : envoie l'historique + le message, laisse le modèle
     * appeler les outils en boucle, et renvoie la réponse finale.
     *
     * @param imageBase64 image encodée en base64 (sans préfixe data:), ou null
     */
    AgentResult run(String systemPrompt, List<ChatTurn> history, String userText,
                    String imageBase64, String imageMimeType, List<AgentTool> tools);

    /** True si le fournisseur dispose d'une clé d'API configurée. */
    boolean isConfigured();
}
