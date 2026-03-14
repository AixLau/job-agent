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
    name = "dashboard_recommendations",
    indexes = @Index(name = "idx_dashboard_recommendations_created_at", columnList = "created_at")
)
public class DashboardRecommendationEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private int score;

    @Column(name = "reasons_json", nullable = false, columnDefinition = "text")
    private String reasonsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DashboardRecommendationEntity() {
    }

    public DashboardRecommendationEntity(String id,
                                         String title,
                                         String company,
                                         int score,
                                         String reasonsJson) {
        this(id, title, company, score, reasonsJson, Instant.now());
    }

    public DashboardRecommendationEntity(String id,
                                         String title,
                                         String company,
                                         int score,
                                         String reasonsJson,
                                         Instant createdAt) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.score = score;
        this.reasonsJson = reasonsJson;
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

    public String getTitle() {
        return title;
    }

    public String getCompany() {
        return company;
    }

    public int getScore() {
        return score;
    }

    public String getReasonsJson() {
        return reasonsJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
