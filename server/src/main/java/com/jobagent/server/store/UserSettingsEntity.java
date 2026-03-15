package com.jobagent.server.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "user_settings")
public class UserSettingsEntity {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(nullable = false)
    private String defaultAutomationLevel;

    @Column(nullable = false)
    private boolean autoSendEnabled;

    @Column(nullable = false)
    private boolean highRiskRequiresReview;

    @Column(nullable = false)
    private boolean chatImmediateAutoSend;

    @Column(nullable = false)
    private int dailyActionLimit;

    @Column(nullable = false)
    private Instant updatedAt;

    protected UserSettingsEntity() {
    }

    public UserSettingsEntity(String userId,
                              String defaultAutomationLevel,
                              boolean autoSendEnabled,
                              boolean highRiskRequiresReview,
                              boolean chatImmediateAutoSend,
                              int dailyActionLimit,
                              Instant updatedAt) {
        this.userId = userId;
        this.defaultAutomationLevel = defaultAutomationLevel;
        this.autoSendEnabled = autoSendEnabled;
        this.highRiskRequiresReview = highRiskRequiresReview;
        this.chatImmediateAutoSend = chatImmediateAutoSend;
        this.dailyActionLimit = dailyActionLimit;
        this.updatedAt = updatedAt == null ? Instant.now() : updatedAt;
    }

    @PrePersist
    void ensureUpdatedAt() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    public String getUserId() {
        return userId;
    }

    public String getDefaultAutomationLevel() {
        return defaultAutomationLevel;
    }

    public void setDefaultAutomationLevel(String defaultAutomationLevel) {
        this.defaultAutomationLevel = defaultAutomationLevel;
    }

    public boolean isAutoSendEnabled() {
        return autoSendEnabled;
    }

    public void setAutoSendEnabled(boolean autoSendEnabled) {
        this.autoSendEnabled = autoSendEnabled;
    }

    public boolean isHighRiskRequiresReview() {
        return highRiskRequiresReview;
    }

    public void setHighRiskRequiresReview(boolean highRiskRequiresReview) {
        this.highRiskRequiresReview = highRiskRequiresReview;
    }

    public boolean isChatImmediateAutoSend() {
        return chatImmediateAutoSend;
    }

    public void setChatImmediateAutoSend(boolean chatImmediateAutoSend) {
        this.chatImmediateAutoSend = chatImmediateAutoSend;
    }

    public int getDailyActionLimit() {
        return dailyActionLimit;
    }

    public void setDailyActionLimit(int dailyActionLimit) {
        this.dailyActionLimit = dailyActionLimit;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }
}
