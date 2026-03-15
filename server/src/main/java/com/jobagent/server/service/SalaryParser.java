package com.jobagent.server.service;

import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SalaryParser {

    private static final String NEGOTIABLE = "\u9762\u8bae";
    private static final Pattern RANGE_PATTERN = Pattern.compile("(?i)(\\d+)\\s*[-~]\\s*(\\d+)\\s*k?");
    private static final Pattern PLUS_PATTERN = Pattern.compile("(?i)(\\d+)\\s*k?\\+");

    public RuleResult.Range parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return emptyRange();
        }
        String normalized = raw.trim();
        if (normalized.contains(NEGOTIABLE)) {
            return emptyRange();
        }
        int multiplier = normalized.toLowerCase().contains("k") ? 1000 : 1;
        Matcher range = RANGE_PATTERN.matcher(normalized);
        if (range.find()) {
            return new RuleResult.Range(
                scale(parseInt(range.group(1)), multiplier),
                scale(parseInt(range.group(2)), multiplier)
            );
        }
        Matcher plus = PLUS_PATTERN.matcher(normalized);
        if (plus.find()) {
            return new RuleResult.Range(scale(parseInt(plus.group(1)), multiplier), null);
        }
        return emptyRange();
    }

    private RuleResult.Range emptyRange() {
        return new RuleResult.Range(null, null);
    }

    private Integer parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer scale(Integer value, int multiplier) {
        if (value == null) {
            return null;
        }
        return value * multiplier;
    }
}
