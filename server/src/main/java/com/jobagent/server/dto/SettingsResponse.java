package com.jobagent.server.dto;

public record SettingsResponse(
    String defaultAutomationLevel,
    boolean autoSendEnabled,
    boolean highRiskRequiresReview,
    boolean chatImmediateAutoSend,
    int dailyActionLimit
) {
}
