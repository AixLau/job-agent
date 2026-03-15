package com.jobagent.server.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record ResumeConfirmRequest(
    @NotBlank String content,
    @NotBlank String format,
    String source,
    String fileName,
    Map<String, Object> parsedJson
) {}
