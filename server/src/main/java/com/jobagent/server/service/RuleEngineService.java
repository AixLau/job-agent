package com.jobagent.server.service;

import org.springframework.stereotype.Service;

import java.util.List;

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
        return evaluate(text, config.salary(), config.experience(), config.automationLevel(), List.of());
    }

    public RuleResult evaluate(String text, String ruleConfigJson, List<String> workerTags) {
        RuleConfigParser.RuleConfig config = ruleConfigParser.parse(ruleConfigJson);
        return evaluate(text, config.salary(), config.experience(), config.automationLevel(), workerTags);
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
}
