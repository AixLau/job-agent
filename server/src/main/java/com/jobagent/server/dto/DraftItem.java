package com.jobagent.server.dto;

public record DraftItem(
    String draftId,
    String conversationId,
    String content,
    java.time.Instant createdAt,
    boolean approved
) {}
