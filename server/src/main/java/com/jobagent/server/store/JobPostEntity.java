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
@Table(name = "job_posts",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_job_posts_source_external_id",
        columnNames = {"source", "external_id"}
    ))
public class JobPostEntity {

    @Id
    private String id;

    @Column(name = "task_id", nullable = false)
    private String taskId;

    @Column(nullable = false)
    private String source;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    private String title;
    private String company;
    private String city;
    private String salary;
    private String experience;

    @Lob
    @Column(name = "jd_raw", columnDefinition = "text")
    private String jdRaw;

    @Lob
    @Column(name = "parsed_json", columnDefinition = "text")
    private String parsedJson;

    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected JobPostEntity() {
    }

    public JobPostEntity(String id,
                         String taskId,
                         String source,
                         String externalId,
                         String title,
                         String company,
                         String city,
                         String salary,
                         String experience,
                         String jdRaw,
                         String parsedJson,
                         String status,
                         Instant createdAt) {
        this.id = id;
        this.taskId = taskId;
        this.source = source;
        this.externalId = externalId;
        this.title = title;
        this.company = company;
        this.city = city;
        this.salary = salary;
        this.experience = experience;
        this.jdRaw = jdRaw;
        this.parsedJson = parsedJson;
        this.status = status;
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

    public String getSource() {
        return source;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getTitle() {
        return title;
    }

    public String getCompany() {
        return company;
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

    public String getJdRaw() {
        return jdRaw;
    }

    public String getParsedJson() {
        return parsedJson;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
