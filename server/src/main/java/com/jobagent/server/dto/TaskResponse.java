package com.jobagent.server.dto;

public record TaskResponse(
    String taskId,
    String status,
    String targetRole,
    String city,
    String salary,
    String experience,
    String automationLevel
) {}
