package com.jobagent.server.dto;

import java.util.List;
import java.util.Map;

public record ChatReportRequest(
    String taskId,
    String conversationId,
    List<Map<String, Object>> messages,
    String lastMessageId
) {}
