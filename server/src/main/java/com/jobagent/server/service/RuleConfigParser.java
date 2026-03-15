package com.jobagent.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RuleConfigParser {

    private final ObjectMapper mapper;

    public RuleConfigParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public RuleConfig parse(String json) {
        if (json == null || json.isBlank()) {
            return RuleConfig.empty();
        }
        try {
            Map<String, Object> data = mapper.readValue(json, new TypeReference<>() {});
            return new RuleConfig(
                asText(data.get("salary")),
                asText(data.get("experience")),
                asText(data.get("automationLevel"))
            );
        } catch (Exception ex) {
            return RuleConfig.empty();
        }
    }

    private String asText(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    public record RuleConfig(String salary, String experience, String automationLevel) {
        public static RuleConfig empty() {
            return new RuleConfig(null, null, null);
        }
    }
}
