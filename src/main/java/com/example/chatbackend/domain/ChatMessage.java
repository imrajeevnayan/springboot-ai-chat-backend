package com.example.chatbackend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "messages",
        indexes = @Index(name = "idx_messages_conversation_created", columnList = "conversation_id, created_at")
)
public class ChatMessage {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MessageRole role;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ChatMessage() {
    }

    public ChatMessage(Conversation conversation, MessageRole role, String content, String model) {
        this.id = UUID.randomUUID();
        this.conversation = conversation;
        this.role = role;
        this.content = content;
        this.model = model;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public MessageRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public String getModel() {
        return model;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
