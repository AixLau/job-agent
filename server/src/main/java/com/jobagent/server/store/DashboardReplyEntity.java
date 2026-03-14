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
    indexes = @Index(name = "idx_dashboard_replies_created_at", columnList = "created_at")
)
public class DashboardReplyEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private String intent;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Column(name = "next_action", nullable = false)
    private String nextAction;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DashboardReplyEntity() {
    }

    public DashboardReplyEntity(String id,
                                String company,
                                String intent,
                                String summary,
                                String nextAction) {
        this(id, company, intent, summary, nextAction, Instant.now());
    }

    public DashboardReplyEntity(String id,
                                String company,
                                String intent,
                                String summary,
                                String nextAction,
                                Instant createdAt) {
        this.id = id;
        this.company = company;
        this.intent = intent;
        this.summary = summary;
        this.nextAction = nextAction;
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

    public String getIntent() {
        return intent;
    }

    public String getSummary() {
        return summary;
    }

    public String getNextAction() {
        return nextAction;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
