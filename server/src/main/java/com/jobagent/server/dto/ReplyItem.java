package com.jobagent.server.dto;

public record ReplyItem(
    String conversationId,
    String jobPostId,
    String company,
    String summary,
    String intent,
    java.time.Instant updatedAt
) {
    public ReplyItem(String conversationId,
                     String summary,
                     String intent,
                     java.time.Instant updatedAt) {
        this(conversationId, null, null, summary, intent, updatedAt);
    }
}
