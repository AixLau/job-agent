package com.jobagent.server.dto;

import java.util.Map;

public record WorkerJobMatchRequest(
    String taskId,
    String stage,
    Map<String, Object> jobPost,
    Map<String, Object> resume,
    Map<String, Object> strategy,
    String idempotencyKey
) {}
