package com.example.chatbackend.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openrouter")
public class OpenRouterProperties {

    private String baseUrl = "https://openrouter.ai/api/v1";
    private String apiKey = "";
    private String defaultModel = "openai/gpt-4o-mini";
    private String httpReferer = "http://localhost:8080";
    private String appTitle = "Spring OpenRouter Chat Backend";
    private Duration timeout = Duration.ofSeconds(30);
    private int maxRetries = 2;
    private Duration retryBackoff = Duration.ofMillis(500);
    private int maxContextMessages = 20;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public String getHttpReferer() {
        return httpReferer;
    }

    public void setHttpReferer(String httpReferer) {
        this.httpReferer = httpReferer;
    }

    public String getAppTitle() {
        return appTitle;
    }

    public void setAppTitle(String appTitle) {
        this.appTitle = appTitle;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Duration getRetryBackoff() {
        return retryBackoff;
    }

    public void setRetryBackoff(Duration retryBackoff) {
        this.retryBackoff = retryBackoff;
    }

    public int getMaxContextMessages() {
        return maxContextMessages;
    }

    public void setMaxContextMessages(int maxContextMessages) {
        this.maxContextMessages = maxContextMessages;
    }
}
