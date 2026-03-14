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

    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DashboardDraftEntity() {
    }

    public DashboardDraftEntity(String id,
                                String company,
                                String title,
                                String content) {
        this(id, company, title, content, Instant.now());
    }

    public DashboardDraftEntity(String id,
                                String company,
                                String title,
                                String content,
                                Instant createdAt) {
        this.id = id;
        this.company = company;
        this.title = title;
        this.content = content;
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

    public String getCompany() {
        return company;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
