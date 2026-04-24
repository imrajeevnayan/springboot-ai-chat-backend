package com.example.chatbackend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.chatbackend.dto.ChatRequest;
import com.example.chatbackend.dto.ChatResponse;
import com.example.chatbackend.service.ChatService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatControllerErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    @Test
    void acceptsChatUrlWithTrailingEncodedNewline() throws Exception {
        UUID conversationId = UUID.randomUUID();
        when(chatService.sendMessage(any(ChatRequest.class))).thenReturn(new ChatResponse(
                conversationId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "assistant",
                "Hello",
                "openai/gpt-4o-mini",
                Instant.parse("2026-04-24T16:11:12Z")
        ));

        mockMvc.perform(post("/api/v1/chat/messages%0A")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "user-1",
                                  "prompt": "Hello"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conversationId").value(conversationId.toString()))
                .andExpect(jsonPath("$.role").value("assistant"))
                .andExpect(jsonPath("$.content").value("Hello"));
    }
}
