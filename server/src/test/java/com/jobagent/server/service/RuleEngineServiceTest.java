package com.jobagent.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineServiceTest {

    @Test
    void resolveParsedRangePrefersWorkerParsedJob() {
        RuleEngineService service = new RuleEngineService(
            new SalaryParser(),
            new ExperienceParser(),
            new RiskRuleSet(),
            new AutomationPolicy(),
            new RuleConfigParser(new ObjectMapper())
        );

        Map<String, Object> parsedJob = Map.of(
            "salary_min", 10000,
            "salary_max", 12000,
            "experience_min", 3,
            "experience_max", 5
        );

        RuleResult.ParsedRange range = service.resolveParsedRange(parsedJob, "20k-30k", "5-7年", "raw");

        assertThat(range.salary().min()).isEqualTo(10000);
        assertThat(range.salary().max()).isEqualTo(12000);
        assertThat(range.experience().min()).isEqualTo(3);
        assertThat(range.experience().max()).isEqualTo(5);
    }

    @Test
    void resolveParsedRangeFallsBackToExtractedFields() {
        RuleEngineService service = new RuleEngineService(
            new SalaryParser(),
            new ExperienceParser(),
            new RiskRuleSet(),
            new AutomationPolicy(),
            new RuleConfigParser(new ObjectMapper())
        );

        RuleResult.ParsedRange range = service.resolveParsedRange(null, "20-30k", "5-7年", "raw");

        assertThat(range.salary().min()).isEqualTo(20000);
        assertThat(range.salary().max()).isEqualTo(30000);
        assertThat(range.experience().min()).isEqualTo(5);
        assertThat(range.experience().max()).isEqualTo(7);
    }
}
