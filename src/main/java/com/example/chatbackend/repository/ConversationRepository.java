package com.example.chatbackend.repository;

import com.example.chatbackend.domain.Conversation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByIdAndUserExternalUserId(UUID id, String externalUserId);

    Page<Conversation> findByUserExternalUserIdOrderByUpdatedAtDesc(String externalUserId, Pageable pageable);
}
