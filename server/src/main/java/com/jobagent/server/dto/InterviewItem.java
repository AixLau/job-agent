package com.jobagent.server.dto;

public record InterviewItem(
    String conversationId,
    String company,
    String title,
    String draftId,
    String draftContent,
    String nextAction,
    java.time.Instant scheduledAt
) {}
