package com.jobagent.server.dto;

public record ReplyItem(
    String conversationId,
    String summary,
    String intent,
    java.time.Instant updatedAt
) {}
