package com.jobagent.server.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AutomationPolicy {

    public RuleResult.AutomationAction evaluate(String automationLevel, List<String> riskTags) {
        if (isAutoMode(automationLevel) && (riskTags == null || riskTags.isEmpty())) {
            return RuleResult.AutomationAction.AUTO_SEND;
        }
        return RuleResult.AutomationAction.REQUIRE_REVIEW;
    }

    private boolean isAutoMode(String automationLevel) {
        return automationLevel != null && automationLevel.equalsIgnoreCase("AUTO");
    }
}
