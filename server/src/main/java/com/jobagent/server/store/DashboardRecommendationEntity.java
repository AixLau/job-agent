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

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "job_post_id", nullable = false)
    private String jobPostId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private int score;

    @Column(name = "reasons_json", nullable = false, columnDefinition = "text")
    private String reasonsJson;

    @Column(name = "risks_json", nullable = false, columnDefinition = "text")
    private String risksJson;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DashboardRecommendationEntity() {
    }

    public DashboardRecommendationEntity(String id,
                                         String userId,
                                         String jobPostId,
                                         String title,
                                         String company,
                                         int score,
                                         String reasonsJson,
                                         String risksJson,
                                         String status) {
        this(id, userId, jobPostId, title, company, score, reasonsJson, risksJson, status, Instant.now());
    }

    public DashboardRecommendationEntity(String id,
                                         String userId,
                                         String jobPostId,
                                         String title,
                                         String company,
                                         int score,
                                         String reasonsJson,
                                         String risksJson,
                                         String status,
                                         Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.jobPostId = jobPostId;
        this.title = title;
        this.company = company;
        this.score = score;
        this.reasonsJson = reasonsJson;
        this.risksJson = risksJson;
        this.status = status;
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

    public String getJobPostId() {
        return jobPostId;
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

    public String getRisksJson() {
        return risksJson;
    }

    public String getReasonsJson() {
        return reasonsJson;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
