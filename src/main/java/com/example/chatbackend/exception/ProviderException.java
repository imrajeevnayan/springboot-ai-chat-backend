package com.example.chatbackend.exception;

public class ProviderException extends RuntimeException {

    private final int providerStatusCode;

    public ProviderException(String message, int providerStatusCode) {
        super(message);
        this.providerStatusCode = providerStatusCode;
    }

    public int getProviderStatusCode() {
        return providerStatusCode;
    }
}
