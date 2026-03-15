package com.jobagent.server.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
    name = "user_job_actions",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_job_action_user_post",
        columnNames = {"user_id", "job_post_id"}
    )
)
public class UserJobActionEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "job_post_id", nullable = false)
    private String jobPostId;

    @Column(nullable = false)
    private String source;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserJobActionEntity() {
    }

    public UserJobActionEntity(String id,
                               String userId,
                               String jobPostId,
                               String source,
                               String actionType,
                               Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.jobPostId = jobPostId;
        this.source = source;
        this.actionType = actionType;
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

    public String getUserId() {
        return userId;
    }

    public String getJobPostId() {
        return jobPostId;
    }

    public String getActionType() {
        return actionType;
    }

    public String getSource() {
        return source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
