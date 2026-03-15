package com.jobagent.server.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "conversations",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_conversations_task_external_id",
        columnNames = {"task_id", "external_id"}
    ))
public class ConversationEntity {

    @Id
    private String id;

    @Column(name = "task_id", nullable = false)
    private String taskId;

    @Column(name = "job_post_id")
    private String jobPostId;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    private String status;

    @Column(name = "last_intent")
    private String lastIntent;

    @Column(name = "last_summary")
    private String lastSummary;

    @Column(name = "last_action")
    private String lastAction;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ConversationEntity() {
    }

    public ConversationEntity(String id,
                              String taskId,
                              String jobPostId,
                              String externalId,
                              String status,
                              String lastIntent,
                              String lastSummary,
                              String lastAction,
                              Instant createdAt) {
        this.id = id;
        this.taskId = taskId;
        this.jobPostId = jobPostId;
        this.externalId = externalId;
        this.status = status;
        this.lastIntent = lastIntent;
        this.lastSummary = lastSummary;
        this.lastAction = lastAction;
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

    public String getTaskId() {
        return taskId;
    }

    public String getJobPostId() {
        return jobPostId;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getStatus() {
        return status;
    }

    public String getLastIntent() {
        return lastIntent;
    }

    public String getLastSummary() {
        return lastSummary;
    }

    public String getLastAction() {
        return lastAction;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setJobPostId(String jobPostId) {
        this.jobPostId = jobPostId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setLastIntent(String lastIntent) {
        this.lastIntent = lastIntent;
    }

    public void setLastSummary(String lastSummary) {
        this.lastSummary = lastSummary;
    }

    public void setLastAction(String lastAction) {
        this.lastAction = lastAction;
    }
}
