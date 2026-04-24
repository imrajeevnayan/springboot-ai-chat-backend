package com.example.chatbackend.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatResponse(
        UUID conversationId,
        UUID userMessageId,
        UUID assistantMessageId,
        String role,
        String content,
        String model,
        Instant createdAt
) {
}
