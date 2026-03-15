package com.jobagent.server.dto;

public record InterviewItem(
    String conversationId,
    String company,
    String title,
    java.time.Instant scheduledAt
) {}
