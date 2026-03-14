package com.jobagent.server.dto;

public record ReplyItem(
    String company,
    String intent,
    String summary,
    String nextAction
) {}
