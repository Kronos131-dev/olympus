package com.kronos.olympusfront.network.dto;

public class ChatMessageDto {
    private Long id;
    private String role;     // "USER" / "ASSISTANT" / "SYSTEM"
    private String content;
    private String createdAt;

    public ChatMessageDto() {}

    public ChatMessageDto(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
