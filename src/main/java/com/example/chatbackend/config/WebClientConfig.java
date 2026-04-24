package com.example.chatbackend.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    @Bean
    WebClient openRouterWebClient(OpenRouterProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) properties.getTimeout().toMillis())
                .responseTimeout(properties.getTimeout());

        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .defaultHeader("HTTP-Referer", properties.getHttpReferer())
                .defaultHeader("X-Title", properties.getAppTitle())
                .build();
    }
}
