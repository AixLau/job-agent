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
@Table(name = "job_matches",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_job_matches_task_job_post",
        columnNames = {"task_id", "job_post_id"}
    ))
public class JobMatchEntity {

    @Id
    private String id;

    @Column(name = "task_id", nullable = false)
    private String taskId;

    @Column(name = "job_post_id", nullable = false)
    private String jobPostId;

    private Integer score;

    @Lob
    @Column(columnDefinition = "text")
    private String reasonJson;

    @Lob
    @Column(name = "risk_tags_json", columnDefinition = "text")
    private String riskTagsJson;

    @Lob
    @Column(name = "rule_json", columnDefinition = "text")
    private String ruleJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected JobMatchEntity() {
    }

    public JobMatchEntity(String id,
                          String taskId,
                          String jobPostId,
                          Integer score,
                          String reasonJson,
                          String riskTagsJson,
                          String ruleJson,
                          Instant createdAt) {
        this.id = id;
        this.taskId = taskId;
        this.jobPostId = jobPostId;
        this.score = score;
        this.reasonJson = reasonJson;
        this.riskTagsJson = riskTagsJson;
        this.ruleJson = ruleJson;
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

    public Integer getScore() {
        return score;
    }

    public String getReasonJson() {
        return reasonJson;
    }

    public String getRiskTagsJson() {
        return riskTagsJson;
    }

    public String getRuleJson() {
        return ruleJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
