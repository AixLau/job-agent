package com.jobagent.server.service;

import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ExperienceParser {

    private static final String UNLIMITED = "\u7ecf\u9a8c\u4e0d\u9650";
    private static final Pattern RANGE_PATTERN = Pattern.compile("(\\d+)\\s*[-~]\\s*(\\d+)\\s*\\u5e74");
    private static final Pattern MIN_PATTERN = Pattern.compile("(\\d+)\\s*\\u5e74\\u4ee5\\u4e0a");

    public RuleResult.Range parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return emptyRange();
        }
        String normalized = raw.trim();
        if (normalized.contains(UNLIMITED)) {
            return emptyRange();
        }
        Matcher range = RANGE_PATTERN.matcher(normalized);
        if (range.find()) {
            return new RuleResult.Range(parseInt(range.group(1)), parseInt(range.group(2)));
        }
        Matcher minOnly = MIN_PATTERN.matcher(normalized);
        if (minOnly.find()) {
            return new RuleResult.Range(parseInt(minOnly.group(1)), null);
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
}
