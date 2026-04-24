package com.example.chatbackend;

import com.example.chatbackend.config.ChatProperties;
import com.example.chatbackend.config.OpenRouterProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({OpenRouterProperties.class, ChatProperties.class})
public class ChatBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatBackendApplication.class, args);
    }
}
