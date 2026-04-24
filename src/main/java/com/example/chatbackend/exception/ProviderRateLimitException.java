package com.example.chatbackend.exception;

public class ProviderRateLimitException extends ProviderException {

    public ProviderRateLimitException(String message) {
        super(message, 429);
    }
}
