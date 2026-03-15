package com.jobagent.server.dto;

import java.util.List;

public record AuditListResponse(
    List<AuditItem> items,
    int page,
    int size,
    long total
) {}
