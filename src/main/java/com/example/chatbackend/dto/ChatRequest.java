package com.example.chatbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ChatRequest(
        @NotBlank
        @Size(max = 128)
        String userId,

        UUID conversationId,

        @Size(max = 200)
        String title,

        @NotBlank
        @Size(max = 12_000)
        String prompt,

        @Size(max = 100)
        String model
) {
}
