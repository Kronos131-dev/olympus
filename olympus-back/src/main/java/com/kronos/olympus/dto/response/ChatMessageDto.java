package com.kronos.olympus.dto.response;

import com.kronos.olympus.model.enums.ChatRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private Long id;
    private ChatRole role;
    private String content;
    private LocalDateTime createdAt;
}
