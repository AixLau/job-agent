package com.jobagent.server.dto;

import java.util.Map;

public record PageReportRequest(
    String taskId,
    String pageType,
    String rawText,
    Map<String, Object> extractedJson,
    String sourceUrl,
    String domHash
) {}
