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
            "exp_min", 3,
            "exp_max", 5
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

    @Test
    void resolveParsedRangeFallsBackWhenParsedJobMissingRanges() {
        RuleEngineService service = new RuleEngineService(
            new SalaryParser(),
            new ExperienceParser(),
            new RiskRuleSet(),
            new AutomationPolicy(),
            new RuleConfigParser(new ObjectMapper())
        );

        Map<String, Object> parsedJob = new java.util.HashMap<>();
        parsedJob.put("salary_min", null);
        parsedJob.put("salary_max", null);
        parsedJob.put("exp_min", null);
        parsedJob.put("exp_max", null);

        RuleResult.ParsedRange range = service.resolveParsedRange(parsedJob, "20-30k", "3-5年", "raw");

        assertThat(range.salary().min()).isEqualTo(20000);
        assertThat(range.salary().max()).isEqualTo(30000);
        assertThat(range.experience().min()).isEqualTo(3);
        assertThat(range.experience().max()).isEqualTo(5);
    }

    @Test
    void evaluateWithParsedRangeRejectsHardFilterMismatch() {
        RuleEngineService service = new RuleEngineService(
            new SalaryParser(),
            new ExperienceParser(),
            new RiskRuleSet(),
            new AutomationPolicy(),
            new RuleConfigParser(new ObjectMapper())
        );

        RuleResult result = service.evaluateWithParsedRange(
            "北京 C端 外包",
            """
                {"city":"上海","salary":"20k-30k","experience":"3-5年","exclude":["外包"],"preferences":["B端"],"automationLevel":"AUTO"}
                """,
            java.util.List.of(),
            new RuleResult.ParsedRange(
                new RuleResult.Range(10000, 15000),
                new RuleResult.Range(1, 2)
            )
        );

        assertThat(result.hardFilterPass()).isFalse();
        assertThat(result.automationAction()).isEqualTo(RuleResult.AutomationAction.REQUIRE_REVIEW);
    }

    @Test
    void evaluateWithParsedRangePassesWhenRangesAndKeywordsMatch() {
        RuleEngineService service = new RuleEngineService(
            new SalaryParser(),
            new ExperienceParser(),
            new RiskRuleSet(),
            new AutomationPolicy(),
            new RuleConfigParser(new ObjectMapper())
        );

        RuleResult result = service.evaluateWithParsedRange(
            "上海 B端 产品岗位",
            """
                {"city":"上海","salary":"20k-30k","experience":"3-5年","exclude":["外包"],"preferences":["B端"],"automationLevel":"SEMI"}
                """,
            java.util.List.of(),
            new RuleResult.ParsedRange(
                new RuleResult.Range(20000, 30000),
                new RuleResult.Range(3, 5)
            )
        );

        assertThat(result.hardFilterPass()).isTrue();
    }
}
