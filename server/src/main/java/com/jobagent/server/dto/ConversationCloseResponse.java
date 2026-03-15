package com.jobagent.server.dto;

public record ConversationCloseResponse(
    String status,
    ConversationSummary conversation
) {}
