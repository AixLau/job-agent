package com.jobagent.server.dto;

public record DraftRegenerateResponse(
    String status,
    DraftContent draft
) {}
