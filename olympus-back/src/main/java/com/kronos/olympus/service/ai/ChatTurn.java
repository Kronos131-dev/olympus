package com.kronos.olympus.service.ai;

/**
 * Un tour de conversation simplifié (texte uniquement) ré-hydraté depuis l'historique persisté.
 */
public class ChatTurn {

    private final String role; // "user" ou "assistant"
    private final String content;

    public ChatTurn(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }
}
