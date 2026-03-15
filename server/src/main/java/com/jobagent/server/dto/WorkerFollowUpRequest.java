package com.jobagent.server.dto;

import java.util.List;
import java.util.Map;

public record WorkerFollowUpRequest(
    String taskId,
    String stage,
    Map<String, Object> conversation,
    List<Map<String, Object>> messages,
    String lastMessageId,
    String idempotencyKey
) {}
