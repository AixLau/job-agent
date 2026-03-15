package com.jobagent.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineServiceTest {

    private final SalaryParser salaryParser = new SalaryParser();
    private final ExperienceParser experienceParser = new ExperienceParser();
    private final RiskRuleSet riskRuleSet = new RiskRuleSet();
    private final AutomationPolicy automationPolicy = new AutomationPolicy();
    private final RuleConfigParser ruleConfigParser = new RuleConfigParser(new ObjectMapper());
    private final RuleEngineService ruleEngineService = new RuleEngineService(
        salaryParser,
        experienceParser,
        riskRuleSet,
        automationPolicy,
        ruleConfigParser
    );

    @Test
    void salary_parser_parses_range() {
        RuleResult.Range range = salaryParser.parse("10-20k");
        assertThat(range.min()).isEqualTo(10000);
        assertThat(range.max()).isEqualTo(20000);
    }

    @Test
    void salary_parser_parses_min_only() {
        RuleResult.Range range = salaryParser.parse("20k+");
        assertThat(range.min()).isEqualTo(20000);
        assertThat(range.max()).isNull();
    }

    @Test
    void salary_parser_parses_negotiable() {
        RuleResult.Range range = salaryParser.parse("\u9762\u8bae");
        assertThat(range.min()).isNull();
        assertThat(range.max()).isNull();
    }

    @Test
    void experience_parser_parses_range() {
        RuleResult.Range range = experienceParser.parse("1-3\u5e74");
        assertThat(range.min()).isEqualTo(1);
        assertThat(range.max()).isEqualTo(3);
    }

    @Test
    void experience_parser_parses_min_only() {
        RuleResult.Range range = experienceParser.parse("3\u5e74\u4ee5\u4e0a");
        assertThat(range.min()).isEqualTo(3);
        assertThat(range.max()).isNull();
    }

    @Test
    void experience_parser_parses_unlimited() {
        RuleResult.Range range = experienceParser.parse("\u7ecf\u9a8c\u4e0d\u9650");
        assertThat(range.min()).isNull();
        assertThat(range.max()).isNull();
    }

    @Test
    void rule_config_parser_reads_fields() {
        String json = "{\"salary\":\"10-20k\",\"experience\":\"1-3\\u5e74\",\"automationLevel\":\"AUTO\"}";
        RuleConfigParser.RuleConfig config = ruleConfigParser.parse(json);
        assertThat(config.salary()).isEqualTo("10-20k");
        assertThat(config.experience()).isEqualTo("1-3\u5e74");
        assertThat(config.automationLevel()).isEqualTo("AUTO");
    }

    @Test
    void rule_engine_unions_risk_tags_and_blocks_auto() {
        String text = "\u5916\u5305 \u5916\u5305 \u5927\u5c0f\u5468 \u52a0\u73ed";
        RuleResult result = ruleEngineService.evaluate(
            text,
            "10-20k",
            "1-3\u5e74",
            "AUTO",
            java.util.List.of("worker_tag")
        );
        assertThat(result.riskTags()).containsExactlyInAnyOrder(
            "\u5916\u5305",
            "\u5927\u5c0f\u5468",
            "\u52a0\u73ed",
            "worker_tag"
        );
        assertThat(result.automationAction()).isEqualTo(RuleResult.AutomationAction.REQUIRE_REVIEW);
    }

    @Test
    void rule_engine_allows_auto_when_no_risk() {
        RuleResult result = ruleEngineService.evaluate(
            "clean text",
            "10-20k",
            "1-3\u5e74",
            "AUTO",
            java.util.List.<String>of()
        );
        assertThat(result.automationAction()).isEqualTo(RuleResult.AutomationAction.AUTO_SEND);
    }
}
