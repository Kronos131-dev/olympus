package com.kronos.olympus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDto {
    private Long conversationId;
    private String reply;
    // Résumé lisible des actions effectuées par l'agent (tools appelés)
    private List<String> actionsTaken;
    private String provider;
    private LocalDateTime timestamp;
}
