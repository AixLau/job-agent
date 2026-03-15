package com.jobagent.server.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "messages",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_messages_conversation_external_id",
        columnNames = {"conversation_id", "external_id"}
    ))
public class MessageEntity {

    @Id
    private String id;

    @Column(name = "conversation_id", nullable = false)
    private String conversationId;

    private String role;

    @Lob
    @Column(columnDefinition = "text")
    private String content;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MessageEntity() {
    }

    public MessageEntity(String id,
                         String conversationId,
                         String role,
                         String content,
                         String externalId,
                         Instant createdAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
        this.externalId = externalId;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    @PrePersist
    void ensureCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public String getId() {
        return id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public String getExternalId() {
        return externalId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
