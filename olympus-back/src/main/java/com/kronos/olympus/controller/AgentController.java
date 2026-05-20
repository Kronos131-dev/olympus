package com.kronos.olympus.controller;

import com.kronos.olympus.dto.response.ChatMessageDto;
import com.kronos.olympus.dto.response.ChatResponseDto;
import com.kronos.olympus.dto.response.ConversationSummaryDto;
import com.kronos.olympus.security.UserDetailsImpl;
import com.kronos.olympus.service.ai.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    /**
     * Envoie un message à l'agent (texte + image optionnelle). Multipart pour permettre la photo.
     */
    @PostMapping(value = "/chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChatResponseDto> chat(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam("message") String message,
            @RequestParam(value = "conversationId", required = false) Long conversationId,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        ChatResponseDto response = agentService.chat(userDetails.getUser(), conversationId, message, image);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationSummaryDto>> getConversations(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        return ResponseEntity.ok(agentService.getConversations(userDetails.getUser().getId()));
    }

    @GetMapping("/conversations/{id}")
    public ResponseEntity<List<ChatMessageDto>> getConversation(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long id) {

        return ResponseEntity.ok(agentService.getConversationMessages(id, userDetails.getUser().getId()));
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<Void> deleteConversation(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long id) {

        agentService.deleteConversation(id, userDetails.getUser().getId());
        return ResponseEntity.noContent().build();
    }
}
