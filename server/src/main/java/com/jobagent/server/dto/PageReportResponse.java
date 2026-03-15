package com.jobagent.server.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageReportResponse(
    String status,
    AnalysisResult analysis,
    DraftItem draft
) {}
