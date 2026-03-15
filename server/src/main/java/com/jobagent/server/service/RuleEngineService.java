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
        RuleResult.Range salaryRange = salaryParser.parse(salaryText);
        RuleResult.Range experienceRange = experienceParser.parse(experienceText);
        List<String> riskTags = riskRuleSet.match(text);
        RuleResult.AutomationAction action = automationPolicy.evaluate(automationLevel, riskTags);
        RuleResult.ParsedRange parsedRange = new RuleResult.ParsedRange(salaryRange, experienceRange);
        return new RuleResult(true, riskTags, action, parsedRange);
    }

    public RuleResult evaluate(String text, String strategyJson) {
        RuleConfigParser.RuleConfig config = ruleConfigParser.parse(strategyJson);
        return evaluate(text, config.salary(), config.experience(), config.automationLevel());
    }
}
