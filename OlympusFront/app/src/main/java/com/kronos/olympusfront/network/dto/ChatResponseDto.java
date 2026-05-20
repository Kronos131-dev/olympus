package com.kronos.olympusfront.network.dto;

import java.util.List;

public class ChatResponseDto {
    private Long conversationId;
    private String reply;
    private List<String> actionsTaken;
    private String provider;
    private String timestamp;

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    public List<String> getActionsTaken() { return actionsTaken; }
    public void setActionsTaken(List<String> actionsTaken) { this.actionsTaken = actionsTaken; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
