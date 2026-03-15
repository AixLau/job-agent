package com.jobagent.server.dto;

import jakarta.validation.constraints.NotBlank;

public record ResumeRequest(
    @NotBlank String content,
    @NotBlank String format,
    String source
) {}
