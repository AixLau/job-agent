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
    name = "dashboard_replies",
    indexes = @Index(name = "idx_dashboard_replies_updated_at", columnList = "updated_at")
)
public class DashboardReplyEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "conversation_id", nullable = false)
    private String conversationId;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Column(nullable = false)
    private String intent;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DashboardReplyEntity() {
    }

    public DashboardReplyEntity(String id,
                                String userId,
                                String conversationId,
                                String summary,
                                String intent) {
        this(id, userId, conversationId, summary, intent, Instant.now());
    }

    public DashboardReplyEntity(String id,
                                String userId,
                                String conversationId,
                                String summary,
                                String intent,
                                Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.conversationId = conversationId;
        this.summary = summary;
        this.intent = intent;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    void ensureCreatedAt() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
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

    public String getSummary() {
        return summary;
    }

    public String getIntent() {
        return intent;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
