package com.jobagent.server.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.server.dto.TaskCreateRequest;
import com.jobagent.server.repository.TaskRepository;
import com.jobagent.server.service.RuleConfigParser;
import com.jobagent.server.service.StrategyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TaskStoreTest.TestConfig.class)
class TaskStoreTest {

    @Autowired
    private TaskStore store;

    @Autowired
    private TaskRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createPersistsRuleConfigJson() throws Exception {
        TaskCreateRequest request = new TaskCreateRequest(
            "title",
            "city",
            "20k",
            "3y",
            List.of(),
            List.of(),
            "AUTO",
            "strategy"
        );

        var response = store.create(request, "user-1");
        TaskEntity entity = repository.findById(response.id()).orElseThrow();
        Field field = TaskEntity.class.getDeclaredField("ruleConfigJson");
        field.setAccessible(true);
        String json = (String) field.get(entity);

        Map<String, Object> config = objectMapper.readValue(json, new TypeReference<>() {});
        assertThat(config.get("salary")).isEqualTo("20k");
        assertThat(config.get("experience")).isEqualTo("3y");
        assertThat(config.get("automationLevel")).isEqualTo("AUTO");
    }

    @Test
    void createUsesStructuredStrategyFieldsWhenRequestFieldsMissing() throws Exception {
        TaskCreateRequest request = new TaskCreateRequest(
            null,
            null,
            null,
            null,
            List.of(),
            List.of(),
            null,
            "上海 产品经理 20k-30k 3-5年 排除外包 偏好B端 AUTO"
        );

        var response = store.create(request, "user-1");
        TaskEntity entity = repository.findById(response.id()).orElseThrow();

        assertThat(entity.getTitle()).isEqualTo("产品经理");
        assertThat(entity.getCity()).isEqualTo("上海");
        assertThat(entity.getSalary()).isEqualTo("20k-30k");
        assertThat(entity.getExperience()).isEqualTo("3-5年");
        assertThat(entity.getAutomationLevel()).isEqualTo("AUTO");
        assertThat(entity.getExcludeJson()).contains("外包");
        assertThat(entity.getPreferencesJson()).contains("B端");
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        StrategyService strategyService() {
            return new StubStrategyService();
        }

        @Bean
        RuleConfigParser ruleConfigParser(ObjectMapper mapper) {
            return new RuleConfigParser(mapper);
        }

        @Bean
        TaskStore taskStore(TaskRepository repository,
                            StrategyService strategyService,
                            RuleConfigParser ruleConfigParser,
                            ObjectMapper mapper) {
            return new TaskStore(repository, strategyService, ruleConfigParser, mapper);
        }
    }

    static class StubStrategyService extends StrategyService {
        StubStrategyService() {
            super(null);
        }

        @Override
        public String parse(String strategyText, String taskId) {
            if (strategyText.contains("上海 产品经理")) {
                return "{\"title\":\"产品经理\",\"city\":\"上海\",\"salary\":\"20k-30k\",\"experience\":\"3-5年\",\"automationLevel\":\"AUTO\",\"exclude\":[\"外包\"],\"preferences\":[\"B端\"],\"raw\":\"" + strategyText + "\"}";
            }
            return "{\"salary\":\"20k\",\"experience\":\"3y\",\"automationLevel\":\"AUTO\"}";
        }
    }
}
