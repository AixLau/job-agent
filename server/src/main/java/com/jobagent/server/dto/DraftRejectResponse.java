package com.jobagent.server.dto;

public record DraftRejectResponse(
    String status,
    DraftSummary draft
) {}
