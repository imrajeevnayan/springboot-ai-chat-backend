package com.example.chatbackend.exception;

public class ProviderTimeoutException extends ProviderException {

    public ProviderTimeoutException(String message) {
        super(message, 504);
    }
}
