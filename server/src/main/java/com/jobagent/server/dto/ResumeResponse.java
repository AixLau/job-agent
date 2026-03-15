package com.jobagent.server.dto;

import java.time.Instant;
import java.util.Map;

public record ResumeResponse(
    ResumePayload resume
) {
    public record ResumePayload(
        String id,
        Map<String, Object> parsedJson,
        Instant createdAt
    ) {}
}
