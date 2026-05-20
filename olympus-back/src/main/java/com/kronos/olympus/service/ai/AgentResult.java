package com.kronos.olympus.service.ai;

import java.util.List;

/**
 * Résultat d'un tour de l'agent : la réponse textuelle finale + les tools exécutés.
 */
public class AgentResult {

    private final String reply;
    private final List<String> actionsTaken;

    public AgentResult(String reply, List<String> actionsTaken) {
        this.reply = reply;
        this.actionsTaken = actionsTaken;
    }

    public String getReply() {
        return reply;
    }

    public List<String> getActionsTaken() {
        return actionsTaken;
    }
}
