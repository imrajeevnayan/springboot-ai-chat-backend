package com.example.chatbackend.dto;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        String role,
        String content,
        String model,
        Instant createdAt
) {
}
