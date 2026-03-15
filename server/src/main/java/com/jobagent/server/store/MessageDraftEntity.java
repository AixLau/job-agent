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
@Table(name = "message_drafts",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_message_drafts_conversation_source_type",
        columnNames = {"conversation_id", "source_type"}
    ))
public class MessageDraftEntity {

    @Id
    private String id;

    @Column(name = "conversation_id", nullable = false)
    private String conversationId;

    @Lob
    @Column(columnDefinition = "text")
    private String content;

    @Column(name = "source_type", nullable = false)
    private String sourceType;

    @Column(nullable = false)
    private boolean approved;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MessageDraftEntity() {
    }

    public MessageDraftEntity(String id,
                              String conversationId,
                              String content,
                              String sourceType,
                              boolean approved,
                              Instant createdAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.content = content;
        this.sourceType = sourceType;
        this.approved = approved;
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

    public String getContent() {
        return content;
    }

    public String getSourceType() {
        return sourceType;
    }

    public boolean isApproved() {
        return approved;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
