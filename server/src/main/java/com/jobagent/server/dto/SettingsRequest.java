package com.jobagent.server.dto;

public record SettingsRequest(
    String defaultAutomationLevel,
    Boolean autoSendEnabled,
    Boolean highRiskRequiresReview,
    Boolean chatImmediateAutoSend,
    Integer dailyActionLimit
) {
}
