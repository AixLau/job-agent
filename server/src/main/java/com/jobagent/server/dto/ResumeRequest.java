package com.jobagent.server.dto;

import java.util.Map;

public record ResumeRequest(
    String content,
    Map<String, Object> parsedJson
) {}
