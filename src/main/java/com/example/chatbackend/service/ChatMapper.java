package com.example.chatbackend.service;

import com.example.chatbackend.domain.ChatMessage;
import com.example.chatbackend.domain.Conversation;
import com.example.chatbackend.dto.ConversationResponse;
import com.example.chatbackend.dto.MessageResponse;
import org.springframework.stereotype.Component;

@Component
class ChatMapper {

    ConversationResponse toConversationResponse(Conversation conversation) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    MessageResponse toMessageResponse(ChatMessage message) {
        return new MessageResponse(
                message.getId(),
                message.getRole().name().toLowerCase(),
                message.getContent(),
                message.getModel(),
                message.getCreatedAt()
        );
    }
}
