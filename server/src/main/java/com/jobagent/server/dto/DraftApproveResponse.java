package com.jobagent.server.dto;

public record DraftApproveResponse(
    String status,
    DraftSummary draft,
    ActionHint actionHint
) {}
