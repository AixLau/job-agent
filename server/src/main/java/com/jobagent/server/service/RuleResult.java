package com.jobagent.server.service;

import java.util.List;

public record RuleResult(
    boolean hardFilterPass,
    List<String> riskTags,
    AutomationAction automationAction,
    ParsedRange parsedRange
) {

    public record ParsedRange(Range salary, Range experience) {
    }

    public record Range(Integer min, Integer max) {
    }

    public enum AutomationAction {
        AUTO_SEND,
        REQUIRE_REVIEW
    }
}
