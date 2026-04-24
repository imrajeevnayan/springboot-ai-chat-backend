package com.example.chatbackend.openrouter;

public record OpenRouterMessage(
        String role,
        String content
) {
}
