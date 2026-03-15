package com.jobagent.server.dto;

public record WorkerReplyClassifyResponse(
    String intent,
    String summary,
    String nextAction
) {}
