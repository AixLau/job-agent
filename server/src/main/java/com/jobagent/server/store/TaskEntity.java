package com.jobagent.server.store;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "job_tasks")
public class TaskEntity {

    @Id
    private String id;

    private String targetRole;
    private String city;
    private String salary;
    private String experience;
    private String automationLevel;
    private String status;

    protected TaskEntity() {
    }

    public TaskEntity(String id,
                      String targetRole,
                      String city,
                      String salary,
                      String experience,
                      String automationLevel,
                      String status) {
        this.id = id;
        this.targetRole = targetRole;
        this.city = city;
        this.salary = salary;
        this.experience = experience;
        this.automationLevel = automationLevel;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getTargetRole() {
        return targetRole;
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
}
