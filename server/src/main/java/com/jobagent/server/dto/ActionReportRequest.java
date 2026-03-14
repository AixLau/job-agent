package com.jobagent.server.dto;

import java.util.Map;

public record ActionReportRequest(
    String taskId,
    String actionType,
    String status,
    Map<String, Object> payload
) {}
