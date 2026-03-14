package com.jobagent.server.dto;

public record TaskCreateRequest(
    String targetRole,
    String city,
    String salary,
    String experience,
    String automationLevel
) {}
