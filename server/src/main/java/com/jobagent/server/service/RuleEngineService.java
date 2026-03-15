package com.jobagent.server.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RuleEngineService {

    private final SalaryParser salaryParser;
    private final ExperienceParser experienceParser;
    private final RiskRuleSet riskRuleSet;
    private final AutomationPolicy automationPolicy;
    private final RuleConfigParser ruleConfigParser;

    public RuleEngineService(SalaryParser salaryParser,
                             ExperienceParser experienceParser,
                             RiskRuleSet riskRuleSet,
                             AutomationPolicy automationPolicy,
                             RuleConfigParser ruleConfigParser) {
        this.salaryParser = salaryParser;
        this.experienceParser = experienceParser;
        this.riskRuleSet = riskRuleSet;
        this.automationPolicy = automationPolicy;
        this.ruleConfigParser = ruleConfigParser;
    }

    public RuleResult evaluate(String text,
                               String salaryText,
                               String experienceText,
                               String automationLevel) {
        return evaluate(text, salaryText, experienceText, automationLevel, List.of());
    }

    public RuleResult evaluate(String text,
                               String salaryText,
                               String experienceText,
                               String automationLevel,
                               List<String> workerTags) {
        RuleResult.Range salaryRange = salaryParser.parse(salaryText);
        RuleResult.Range experienceRange = experienceParser.parse(experienceText);
        List<String> riskTags = unionTags(riskRuleSet.match(text), workerTags);
        RuleResult.AutomationAction action = automationPolicy.evaluate(automationLevel, riskTags);
        RuleResult.ParsedRange parsedRange = new RuleResult.ParsedRange(salaryRange, experienceRange);
        return new RuleResult(true, riskTags, action, parsedRange);
    }

    public RuleResult evaluate(String text, String strategyJson) {
        RuleConfigParser.RuleConfig config = ruleConfigParser.parse(strategyJson);
        RuleResult.ParsedRange parsedRange = new RuleResult.ParsedRange(
            salaryParser.parse(text),
            experienceParser.parse(text)
        );
        return evaluateWithConfig(text, config, List.of(), parsedRange);
    }

    public RuleResult evaluate(String text, String ruleConfigJson, List<String> workerTags) {
        RuleConfigParser.RuleConfig config = ruleConfigParser.parse(ruleConfigJson);
        RuleResult.ParsedRange parsedRange = new RuleResult.ParsedRange(
            salaryParser.parse(text),
            experienceParser.parse(text)
        );
        return evaluateWithConfig(text, config, workerTags, parsedRange);
    }

    public RuleResult evaluateWithParsedRange(String text,
                                              String ruleConfigJson,
                                              List<String> workerTags,
                                              RuleResult.ParsedRange parsedRange) {
        RuleConfigParser.RuleConfig config = ruleConfigParser.parse(ruleConfigJson);
        return evaluateWithConfig(text, config, workerTags, parsedRange);
    }

    private RuleResult evaluateWithConfig(String text,
                                          RuleConfigParser.RuleConfig config,
                                          List<String> workerTags,
                                          RuleResult.ParsedRange parsedRange) {
        List<String> riskTags = unionTags(riskRuleSet.match(text), workerTags);
        RuleResult.AutomationAction action = automationPolicy.evaluate(config.automationLevel(), riskTags);
        boolean hardFilterPass = matchesHardFilter(text, config, parsedRange);
        if (!hardFilterPass) {
            action = RuleResult.AutomationAction.REQUIRE_REVIEW;
        }
        return new RuleResult(hardFilterPass, riskTags, action, parsedRange);
    }

    public RuleResult.ParsedRange resolveParsedRange(Map<String, Object> parsedJob,
                                                     String salaryText,
                                                     String experienceText,
                                                     String rawText) {
        if (parsedJob != null && !parsedJob.isEmpty()) {
            Integer salaryMin = readInt(parsedJob, "salary_min", "salaryMin");
            Integer salaryMax = readInt(parsedJob, "salary_max", "salaryMax");
            Integer expMin = readInt(parsedJob, "exp_min", "expMin", "experience_min", "experienceMin");
            Integer expMax = readInt(parsedJob, "exp_max", "expMax", "experience_max", "experienceMax");
            if (salaryMin != null || salaryMax != null || expMin != null || expMax != null) {
                RuleResult.Range salary = new RuleResult.Range(salaryMin, salaryMax);
                RuleResult.Range experience = new RuleResult.Range(expMin, expMax);
                return new RuleResult.ParsedRange(salary, experience);
            }
        }
        String salarySource = firstNonBlank(salaryText, rawText);
        String experienceSource = firstNonBlank(experienceText, rawText);
        RuleResult.Range salary = salaryParser.parse(salarySource);
        RuleResult.Range experience = experienceParser.parse(experienceSource);
        return new RuleResult.ParsedRange(salary, experience);
    }

    private List<String> unionTags(List<String> primary, List<String> secondary) {
        if ((primary == null || primary.isEmpty()) && (secondary == null || secondary.isEmpty())) {
            return List.of();
        }
        java.util.LinkedHashSet<String> merged = new java.util.LinkedHashSet<>();
        if (primary != null) {
            merged.addAll(primary);
        }
        if (secondary != null) {
            merged.addAll(secondary);
        }
        return List.copyOf(merged);
    }

    private Integer readInt(Map<String, Object> data, String... keys) {
        if (data == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            Object value = data.get(key);
            Integer parsed = toInt(value);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private Integer toInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return "";
    }

    private boolean matchesHardFilter(String text,
                                      RuleConfigParser.RuleConfig config,
                                      RuleResult.ParsedRange parsedRange) {
        return matchesCity(text, config.city())
            && matchesRange(salaryParser.parse(config.salary()), parsedRange == null ? null : parsedRange.salary())
            && matchesRange(experienceParser.parse(config.experience()), parsedRange == null ? null : parsedRange.experience())
            && excludesAllowed(text, config.exclude())
            && matchesPreferences(text, config.preferences());
    }

    private boolean matchesCity(String text, String city) {
        if (city == null || city.isBlank()) {
            return true;
        }
        if (text == null || text.isBlank()) {
            return true;
        }
        return text.contains(city);
    }

    private boolean matchesRange(RuleResult.Range expected, RuleResult.Range actual) {
        if (isEmpty(expected) || actual == null || isEmpty(actual)) {
            return true;
        }
        int expectedMin = expected.min() == null ? Integer.MIN_VALUE : expected.min();
        int expectedMax = expected.max() == null ? Integer.MAX_VALUE : expected.max();
        int actualMin = actual.min() == null ? Integer.MIN_VALUE : actual.min();
        int actualMax = actual.max() == null ? Integer.MAX_VALUE : actual.max();
        return expectedMin <= actualMax && actualMin <= expectedMax;
    }

    private boolean isEmpty(RuleResult.Range range) {
        return range == null || (range.min() == null && range.max() == null);
    }

    private boolean excludesAllowed(String text, List<String> excludes) {
        if (excludes == null || excludes.isEmpty() || text == null || text.isBlank()) {
            return true;
        }
        return excludes.stream().noneMatch(token -> token != null && !token.isBlank() && text.contains(token));
    }

    private boolean matchesPreferences(String text, List<String> preferences) {
        if (preferences == null || preferences.isEmpty()) {
            return true;
        }
        if (text == null || text.isBlank()) {
            return false;
        }
        return preferences.stream().anyMatch(token -> token != null && !token.isBlank() && text.contains(token));
    }
}
