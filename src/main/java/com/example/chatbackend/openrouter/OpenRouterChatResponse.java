package com.example.chatbackend.openrouter;

import java.util.List;

public record OpenRouterChatResponse(
        String id,
        String model,
        List<Choice> choices
) {

    public record Choice(OpenRouterMessage message) {
    }
}
