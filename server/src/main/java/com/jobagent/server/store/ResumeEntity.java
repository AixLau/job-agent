package com.jobagent.server.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "resumes")
public class ResumeEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(nullable = false, columnDefinition = "text")
    private String parsedJson;

    @Column(nullable = false)
    private Instant createdAt;

    protected ResumeEntity() {
    }

    public ResumeEntity(String id, String userId, String content, String parsedJson) {
        this.id = id;
        this.userId = userId;
        this.content = content;
        this.parsedJson = parsedJson;
        this.createdAt = Instant.now();
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

    public String getContent() {
        return content;
    }

    public String getParsedJson() {
        return parsedJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
