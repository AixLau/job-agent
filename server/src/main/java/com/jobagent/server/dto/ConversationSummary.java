package com.jobagent.server.dto;

public record ConversationSummary(
    String id,
    String status,
    String lastIntent,
    String lastSummary
) {}
