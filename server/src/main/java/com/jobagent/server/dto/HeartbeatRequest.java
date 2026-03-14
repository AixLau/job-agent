package com.jobagent.server.dto;

public record HeartbeatRequest(
    String userId,
    String taskId,
    String tabId,
    String status,
    Long ts
) {}
