package com.jobagent.server.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String actionType;

    @Column(columnDefinition = "text")
    private String payload;

    private String result;

    @Column(columnDefinition = "text")
    private String modelOutput;

    @Column(columnDefinition = "text")
    private String riskTagsJson;

    @Column(nullable = false)
    private Instant createdAt;

    protected AuditLogEntity() {
    }

    public AuditLogEntity(String id, String userId, String actionType, String payload) {
        this(id, userId, actionType, payload, "OK", null, "[]", Instant.now());
    }

    public AuditLogEntity(String id, String userId, String actionType, String payload, Instant createdAt) {
        this(id, userId, actionType, payload, "OK", null, "[]", createdAt);
    }

    public AuditLogEntity(String id,
                          String userId,
                          String actionType,
                          String payload,
                          String result,
                          String modelOutput,
                          String riskTagsJson) {
        this(id, userId, actionType, payload, result, modelOutput, riskTagsJson, Instant.now());
    }

    public AuditLogEntity(String id,
                          String userId,
                          String actionType,
                          String payload,
                          String result,
                          String modelOutput,
                          String riskTagsJson,
                          Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.actionType = actionType;
        this.payload = payload;
        this.result = result;
        this.modelOutput = modelOutput;
        this.riskTagsJson = riskTagsJson == null || riskTagsJson.isBlank() ? "[]" : riskTagsJson;
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

    public String getActionType() {
        return actionType;
    }

    public String getPayload() {
        return payload;
    }

    public String getResult() {
        return result;
    }

    public String getModelOutput() {
        return modelOutput;
    }

    public String getRiskTagsJson() {
        return riskTagsJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
