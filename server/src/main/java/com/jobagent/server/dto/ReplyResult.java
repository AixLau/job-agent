package com.jobagent.server.dto;

public record ReplyResult(
    String intent,
    String summary,
    String nextAction
) {}
