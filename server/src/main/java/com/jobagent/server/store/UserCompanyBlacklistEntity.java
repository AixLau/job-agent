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
    name = "user_company_blacklist",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_blacklist_user_company_source",
        columnNames = {"user_id", "company_name", "source"}
    )
)
public class UserCompanyBlacklistEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(nullable = false)
    private String source;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserCompanyBlacklistEntity() {
    }

    public UserCompanyBlacklistEntity(String id,
                                      String userId,
                                      String companyName,
                                      String source,
                                      Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.companyName = companyName;
        this.source = source;
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

    public String getCompanyName() {
        return companyName;
    }

    public String getSource() {
        return source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
