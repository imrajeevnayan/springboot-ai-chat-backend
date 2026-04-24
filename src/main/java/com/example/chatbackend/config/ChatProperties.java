package com.example.chatbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chat")
public class ChatProperties {

    private final RateLimit rateLimit = new RateLimit();
    private final Prompt prompt = new Prompt();

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public Prompt getPrompt() {
        return prompt;
    }

    public static class RateLimit {
        private int requestsPerMinute = 30;

        public int getRequestsPerMinute() {
            return requestsPerMinute;
        }

        public void setRequestsPerMinute(int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }
    }

    public static class Prompt {
        private int maxLength = 12_000;

        public int getMaxLength() {
            return maxLength;
        }

        public void setMaxLength(int maxLength) {
            this.maxLength = maxLength;
        }
    }
}
