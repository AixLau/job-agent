package com.jobagent.server.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
    name = "dashboard_drafts",
    indexes = @Index(name = "idx_dashboard_drafts_created_at", columnList = "created_at")
)
public class DashboardDraftEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "conversation_id", nullable = false)
    private String conversationId;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(nullable = false)
    private boolean approved;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DashboardDraftEntity() {
    }

    public DashboardDraftEntity(String id,
                                String userId,
                                String conversationId,
                                String content,
                                boolean approved) {
        this(id, userId, conversationId, content, approved, Instant.now());
    }

    public DashboardDraftEntity(String id,
                                String userId,
                                String conversationId,
                                String content,
                                boolean approved,
                                Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.conversationId = conversationId;
        this.content = content;
        this.approved = approved;
        this.createdAt = createdAt;
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

    public String getUserId() {
        return userId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getContent() {
        return content;
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
}
