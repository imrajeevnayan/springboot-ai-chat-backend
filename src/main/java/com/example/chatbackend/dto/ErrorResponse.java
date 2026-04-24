package com.example.chatbackend.dto;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        String error,
        String message,
        Map<String, Object> details,
        Instant timestamp,
        String path
) {
}
