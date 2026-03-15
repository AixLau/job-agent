package com.jobagent.server.dto;

import java.time.Instant;

public record TaskResponse(
    String id,
    String status,
    String title,
    String city,
    String salary,
    String experience,
    String automationLevel,
    String strategyJson,
    Instant createdAt
) {}
