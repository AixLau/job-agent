package com.jobagent.server.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "job_tasks")
public class TaskEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String userId;

    private String title;
    private String city;
    private String salary;
    private String experience;
    private String automationLevel;
    private String status;
    @Lob
    private String strategyJson;

    @Lob
    private String ruleConfigJson;

    @Lob
    private String excludeJson;

    @Lob
    private String preferencesJson;

    @Column(nullable = false)
    private Instant createdAt;

    protected TaskEntity() {
    }

    public TaskEntity(String id,
                      String userId,
                      String title,
                      String city,
                      String salary,
                      String experience,
                      String automationLevel,
                      String status,
                      String strategyJson,
                      String ruleConfigJson,
                      String excludeJson,
                      String preferencesJson,
                      Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.city = city;
        this.salary = salary;
        this.experience = experience;
        this.automationLevel = automationLevel;
        this.status = status;
        this.strategyJson = strategyJson;
        this.ruleConfigJson = ruleConfigJson;
        this.excludeJson = excludeJson;
        this.preferencesJson = preferencesJson;
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

    public String getTitle() {
        return title;
    }

    public String getCity() {
        return city;
    }

    public String getSalary() {
        return salary;
    }

    public String getExperience() {
        return experience;
    }

    public String getAutomationLevel() {
        return automationLevel;
    }

    public String getStatus() {
        return status;
    }

    public String getStrategyJson() {
        return strategyJson;
    }

    public String getRuleConfigJson() {
        return ruleConfigJson;
    }

    public String getExcludeJson() {
        return excludeJson;
    }

    public String getPreferencesJson() {
        return preferencesJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setSalary(String salary) {
        this.salary = salary;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public void setAutomationLevel(String automationLevel) {
        this.automationLevel = automationLevel;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setStrategyJson(String strategyJson) {
        this.strategyJson = strategyJson;
    }

    public void setRuleConfigJson(String ruleConfigJson) {
        this.ruleConfigJson = ruleConfigJson;
    }

    public void setExcludeJson(String excludeJson) {
        this.excludeJson = excludeJson;
    }

    public void setPreferencesJson(String preferencesJson) {
        this.preferencesJson = preferencesJson;
    }
}
