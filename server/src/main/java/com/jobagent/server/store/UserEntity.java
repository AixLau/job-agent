package com.jobagent.server.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String account;

    @Column(nullable = false)
    private String passwordHash;

    private String email;

    @Column(nullable = false)
    private String profileStatus;

    @Column(nullable = false)
    private Instant createdAt;

    protected UserEntity() {
    }

    public UserEntity(String id, String account, String passwordHash, String email, String profileStatus) {
        this.id = id;
        this.account = account;
        this.passwordHash = passwordHash;
        this.email = email;
        this.profileStatus = profileStatus == null ? "INCOMPLETE" : profileStatus;
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

    public String getAccount() {
        return account;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public String getProfileStatus() {
        return profileStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
