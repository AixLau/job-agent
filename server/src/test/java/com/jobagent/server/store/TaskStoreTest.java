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
            return "{\"salary\":\"20k\",\"experience\":\"3y\",\"automationLevel\":\"AUTO\"}";
        }
    }
}
