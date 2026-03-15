package com.jobagent.server.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RiskRuleSet {

    private static final List<RiskRule> RULES = List.of(
        new RiskRule("\u5916\u5305", List.of("\u5916\u5305")),
        new RiskRule("\u5927\u5c0f\u5468", List.of("\u5927\u5c0f\u5468")),
        new RiskRule("\u52a0\u73ed", List.of("\u52a0\u73ed")),
        new RiskRule("\u7070\u4ea7", List.of("\u7070\u4ea7")),
        new RiskRule("\u4fdd\u8bc1\u5f55\u7528", List.of("\u4fdd\u8bc1\u5f55\u7528")),
        new RiskRule("\u654f\u611f\u627f\u8bfa", List.of("\u654f\u611f\u627f\u8bfa"))
    );

    public List<String> match(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        Set<String> matched = new LinkedHashSet<>();
        for (RiskRule rule : RULES) {
            for (String keyword : rule.keywords()) {
                if (normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                    matched.add(rule.tag());
                    break;
                }
            }
        }
        return List.copyOf(matched);
    }

    private record RiskRule(String tag, List<String> keywords) {
    }
}
