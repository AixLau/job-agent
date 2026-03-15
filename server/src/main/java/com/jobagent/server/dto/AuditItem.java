package com.jobagent.server.dto;

import java.time.Instant;
import java.util.List;

public record AuditItem(
    String actionType,
    Instant createdAt,
    String result,
    String payload,
    String modelOutput,
    List<String> riskTags
) {}
