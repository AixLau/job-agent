package com.jobagent.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
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
                asText(data.get("title")),
                asText(data.get("city")),
                asText(data.get("salary")),
                asText(data.get("experience")),
                asText(data.get("automationLevel")),
                asStringList(data.get("exclude")),
                asStringList(data.get("preferences"))
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

    private List<String> asStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                .map(String::valueOf)
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
        }
        return List.of();
    }

    public record RuleConfig(
        String title,
        String city,
        String salary,
        String experience,
        String automationLevel,
        List<String> exclude,
        List<String> preferences
    ) {
        public static RuleConfig empty() {
            return new RuleConfig(null, null, null, null, null, List.of(), List.of());
        }
    }
}
