package com.jobagent.server.dto;

import java.util.Map;

public record WorkerDraftRequest(
    String taskId,
    String stage,
    Map<String, Object> conversation,
    Map<String, Object> jobPost,
    Map<String, Object> resume,
    String idempotencyKey
) {}
