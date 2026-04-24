package com.example.chatbackend.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.chatbackend.config.ChatProperties;
import com.example.chatbackend.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;

class RateLimitServiceTest {

    @Test
    void rejectsRequestsBeyondConfiguredLimit() {
        ChatProperties properties = new ChatProperties();
        properties.getRateLimit().setRequestsPerMinute(1);
        RateLimitService service = new RateLimitService(properties);

        assertDoesNotThrow(() -> service.checkAllowed("user-1"));
        assertThrows(RateLimitExceededException.class, () -> service.checkAllowed("user-1"));
    }

    @Test
    void keepsBucketsSeparatePerUser() {
        ChatProperties properties = new ChatProperties();
        properties.getRateLimit().setRequestsPerMinute(1);
        RateLimitService service = new RateLimitService(properties);

        assertDoesNotThrow(() -> service.checkAllowed("user-1"));
        assertDoesNotThrow(() -> service.checkAllowed("user-2"));
    }
}
