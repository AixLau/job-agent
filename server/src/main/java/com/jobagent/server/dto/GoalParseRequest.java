package com.jobagent.server.dto;

public record GoalParseRequest(
    String taskId,
    String stage,
    String strategyText,
    String idempotencyKey
) {}
