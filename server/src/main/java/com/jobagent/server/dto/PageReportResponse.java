package com.jobagent.server.dto;

public record PageReportResponse(
    String status,
    AnalysisResult analysis,
    DraftItem draft
) {}
