package com.example.chatbackend.controller;

import com.example.chatbackend.dto.ChatRequest;
import com.example.chatbackend.dto.ChatResponse;
import com.example.chatbackend.dto.ConversationResponse;
import com.example.chatbackend.dto.MessageResponse;
import com.example.chatbackend.dto.PageResponse;
import com.example.chatbackend.service.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat/messages")
    ResponseEntity<ChatResponse> sendMessage(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = chatService.sendMessage(request);
        URI location = URI.create("/api/v1/conversations/" + response.conversationId() + "/messages");
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/users/{userId}/conversations")
    PageResponse<ConversationResponse> listConversations(
            @PathVariable @NotBlank String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = ChatService.boundedPageable(page, size);
        return chatService.listConversations(userId, pageable);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    PageResponse<MessageResponse> listMessages(
            @PathVariable UUID conversationId,
            @RequestParam @NotBlank String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Pageable pageable = ChatService.boundedPageable(page, size);
        return chatService.listMessages(userId, conversationId, pageable);
    }

    @DeleteMapping("/conversations/{conversationId}")
    ResponseEntity<Void> deleteConversation(
            @PathVariable UUID conversationId,
            @RequestParam @NotBlank String userId
    ) {
        chatService.deleteConversation(userId, conversationId);
        return ResponseEntity.noContent().build();
    }
}
