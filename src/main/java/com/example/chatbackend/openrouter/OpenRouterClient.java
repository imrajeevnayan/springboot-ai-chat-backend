package com.example.chatbackend.openrouter;

import com.example.chatbackend.config.OpenRouterProperties;
import com.example.chatbackend.exception.ProviderException;
import com.example.chatbackend.exception.ProviderRateLimitException;
import com.example.chatbackend.exception.ProviderTimeoutException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
public class OpenRouterClient {

    private final WebClient webClient;
    private final OpenRouterProperties properties;

    public OpenRouterClient(WebClient openRouterWebClient, OpenRouterProperties properties) {
        this.webClient = openRouterWebClient;
        this.properties = properties;
    }

    public OpenRouterMessage complete(String model, List<OpenRouterMessage> messages) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new ProviderException("OpenRouter API key is not configured", 500);
        }

        OpenRouterChatRequest request = new OpenRouterChatRequest(model, messages);

        OpenRouterChatResponse response = webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(body -> Mono.error(toProviderException(clientResponse.statusCode(), body))))
                .bodyToMono(OpenRouterChatResponse.class)
                .timeout(properties.getTimeout())
                .retryWhen(Retry.backoff(properties.getMaxRetries(), properties.getRetryBackoff())
                        .filter(this::isRetryable))
                .onErrorMap(TimeoutException.class, ignored -> new ProviderTimeoutException("OpenRouter request timed out"))
                .block();

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new ProviderException("OpenRouter returned an empty response", 502);
        }

        OpenRouterMessage message = response.choices().getFirst().message();
        if (message == null || !StringUtils.hasText(message.content())) {
            throw new ProviderException("OpenRouter returned an empty assistant message", 502);
        }

        return message;
    }

    private RuntimeException toProviderException(HttpStatusCode statusCode, String body) {
        int status = statusCode.value();
        if (statusCode.isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)) {
            return new ProviderRateLimitException("OpenRouter rate limit exceeded");
        }
        return new ProviderException("OpenRouter API error: " + summarize(body), status);
    }

    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof ProviderRateLimitException || throwable instanceof ProviderTimeoutException) {
            return true;
        }
        if (throwable instanceof ProviderException providerException) {
            return providerException.getProviderStatusCode() >= 500;
        }
        return throwable instanceof WebClientRequestException || throwable instanceof TimeoutException;
    }

    private String summarize(String body) {
        if (!StringUtils.hasText(body)) {
            return "empty error body";
        }
        return body.length() <= 500 ? body : body.substring(0, 500);
    }
}
