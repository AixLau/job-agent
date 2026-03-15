package com.jobagent.server.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "profiles")
public class ProfileEntity {

    @Id
    private String userId;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String city;

    private Integer yearsExperience;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Column(nullable = false, columnDefinition = "text")
    private String skillsJson;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ProfileEntity() {
    }

    public ProfileEntity(String userId,
                         String fullName,
                         String phone,
                         String city,
                         Integer yearsExperience,
                         String summary,
                         String skillsJson,
                         Instant updatedAt) {
        this.userId = userId;
        this.fullName = defaultString(fullName);
        this.phone = defaultString(phone);
        this.city = defaultString(city);
        this.yearsExperience = yearsExperience;
        this.summary = defaultString(summary);
        this.skillsJson = skillsJson == null || skillsJson.isBlank() ? "[]" : skillsJson;
        this.updatedAt = updatedAt == null ? Instant.now() : updatedAt;
    }

    @PrePersist
    @PreUpdate
    void touchUpdatedAt() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    public String getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = defaultString(fullName);
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = defaultString(phone);
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = defaultString(city);
    }

    public Integer getYearsExperience() {
        return yearsExperience;
    }

    public void setYearsExperience(Integer yearsExperience) {
        this.yearsExperience = yearsExperience;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = defaultString(summary);
    }

    public String getSkillsJson() {
        return skillsJson;
    }

    public void setSkillsJson(String skillsJson) {
        this.skillsJson = skillsJson == null || skillsJson.isBlank() ? "[]" : skillsJson;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    private static String defaultString(String value) {
        return value == null ? "" : value;
    }
}
