package com.example.chatbackend.openrouter;

import java.util.List;

public record OpenRouterChatRequest(
        String model,
        List<OpenRouterMessage> messages
) {
}
