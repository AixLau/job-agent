package com.jobagent.server.dto;

public record WorkerFollowUpResponse(
    String priority,
    String suggestedStatus,
    String nextAction,
    Integer followUpHours,
    String draftContent,
    Boolean requiresReview
) {}
