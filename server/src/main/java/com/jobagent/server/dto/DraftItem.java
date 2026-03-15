package com.jobagent.server.dto;

public record DraftItem(
    String draftId,
    String conversationId,
    String jobPostId,
    String company,
    String content,
    java.time.Instant createdAt,
    boolean approved
) {
    public DraftItem(String draftId,
                     String conversationId,
                     String content,
                     java.time.Instant createdAt,
                     boolean approved) {
        this(draftId, conversationId, null, null, content, createdAt, approved);
    }
}
