package com.jobagent.server.dto;

import java.util.Map;

public record ResumeResponse(
    String resumeId,
    String content,
    Map<String, Object> parsedJson
) {}
