package com.jobagent.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record ChatReportRequest(
    @NotBlank String taskId,
    @NotBlank String conversationId,
    @NotNull List<Map<String, Object>> messages,
    @NotBlank String lastMessageId
) {}
