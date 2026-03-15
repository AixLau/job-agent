package com.jobagent.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record PageReportRequest(
    @NotBlank String taskId,
    @NotBlank String pageType,
    @NotBlank String rawText,
    @NotNull Map<String, Object> extractedJson,
    @NotBlank String sourceUrl,
    @NotBlank String domHash,
    Boolean wantDraft
) {}
