package com.example.chatbackend.repository;

import com.example.chatbackend.domain.ChatMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<ChatMessage, UUID> {

    Page<ChatMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId, Pageable pageable);

    List<ChatMessage> findTop20ByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    void deleteByConversationId(UUID conversationId);
}
