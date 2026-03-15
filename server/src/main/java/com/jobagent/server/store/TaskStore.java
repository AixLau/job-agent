package com.jobagent.server.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.server.dto.TaskCreateRequest;
import com.jobagent.server.dto.TaskResponse;
import com.jobagent.server.dto.TaskUpdateRequest;
import com.jobagent.server.repository.TaskRepository;
import com.jobagent.server.service.RuleConfigParser;
import com.jobagent.server.service.StrategyService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class TaskStore {

    private final TaskRepository repository;
    private final StrategyService strategyService;
    private final RuleConfigParser ruleConfigParser;
    private final ObjectMapper mapper;

    public TaskStore(TaskRepository repository,
                     StrategyService strategyService,
                     RuleConfigParser ruleConfigParser,
                     ObjectMapper mapper) {
        this.repository = repository;
        this.strategyService = strategyService;
        this.ruleConfigParser = ruleConfigParser;
        this.mapper = mapper;
    }

    public TaskResponse create(TaskCreateRequest request, String userId) {
        String id = UUID.randomUUID().toString();
        String strategyJson = strategyService.parse(request.strategyText(), id);
        RuleConfigParser.RuleConfig config = ruleConfigParser.parse(strategyJson);
        String resolvedTitle = firstNonBlank(request.title(), config.title());
        String resolvedCity = firstNonBlank(request.city(), config.city());
        String resolvedSalary = firstNonBlank(request.salary(), config.salary());
        String resolvedExperience = firstNonBlank(request.experience(), config.experience());
        String resolvedAutomationLevel = firstNonBlank(request.automationLevel(), config.automationLevel());
        List<String> resolvedExclude = firstNonEmptyList(request.exclude(), config.exclude());
        List<String> resolvedPreferences = firstNonEmptyList(request.preferences(), config.preferences());
        String ruleConfigJson = buildRuleConfigJson(
            resolvedTitle,
            resolvedCity,
            resolvedSalary,
            resolvedExperience,
            resolvedAutomationLevel,
            resolvedExclude,
            resolvedPreferences
        );
        TaskEntity entity = new TaskEntity(
            id,
            userId,
            resolvedTitle,
            resolvedCity,
            resolvedSalary,
            resolvedExperience,
            resolvedAutomationLevel,
            "ACTIVE",
            strategyJson,
            ruleConfigJson,
            toJson(resolvedExclude),
            toJson(resolvedPreferences),
            Instant.now()
        );
        TaskEntity saved = repository.save(entity);
        return toResponse(saved);
    }

    public TaskResponse update(String id, String userId, TaskUpdateRequest request) {
        TaskEntity entity = repository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "task not found"));
        if (request.title() != null) {
            entity.setTitle(request.title());
        }
        if (request.city() != null) {
            entity.setCity(request.city());
        }
        if (request.salary() != null) {
            entity.setSalary(request.salary());
        }
        if (request.experience() != null) {
            entity.setExperience(request.experience());
        }
        if (request.status() != null) {
            String normalizedStatus = request.status().toUpperCase(Locale.ROOT);
            if (!isAllowedStatus(normalizedStatus)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid status");
            }
            entity.setStatus(normalizedStatus);
        }
        if (request.strategyText() != null) {
            entity.setStrategyJson(strategyService.parse(request.strategyText(), entity.getId()));
        }
        RuleConfigParser.RuleConfig config = ruleConfigParser.parse(entity.getStrategyJson());
        if (request.title() == null && entity.getTitle() == null && config.title() != null) {
            entity.setTitle(config.title());
        }
        if (request.city() == null && entity.getCity() == null && config.city() != null) {
            entity.setCity(config.city());
        }
        if (request.salary() == null && entity.getSalary() == null && config.salary() != null) {
            entity.setSalary(config.salary());
        }
        if (request.experience() == null && entity.getExperience() == null && config.experience() != null) {
            entity.setExperience(config.experience());
        }
        if (request.automationLevel() != null) {
            entity.setAutomationLevel(request.automationLevel());
        } else if (entity.getAutomationLevel() == null && config.automationLevel() != null) {
            entity.setAutomationLevel(config.automationLevel());
        }
        if (request.exclude() != null) {
            entity.setExcludeJson(toJson(request.exclude()));
        } else if (isBlankJsonArray(entity.getExcludeJson()) && !config.exclude().isEmpty()) {
            entity.setExcludeJson(toJson(config.exclude()));
        }
        if (request.preferences() != null) {
            entity.setPreferencesJson(toJson(request.preferences()));
        } else if (isBlankJsonArray(entity.getPreferencesJson()) && !config.preferences().isEmpty()) {
            entity.setPreferencesJson(toJson(config.preferences()));
        }
        entity.setRuleConfigJson(buildRuleConfigJson(
            entity.getTitle(),
            entity.getCity(),
            entity.getSalary(),
            entity.getExperience(),
            entity.getAutomationLevel(),
            parseJsonList(entity.getExcludeJson()),
            parseJsonList(entity.getPreferencesJson())
        ));
        TaskEntity saved = repository.save(entity);
        return toResponse(saved);
    }

    public List<TaskResponse> listForUser(String userId) {
        return repository.findAllByUserId(userId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private TaskResponse toResponse(TaskEntity entity) {
        return new TaskResponse(
            entity.getId(),
            entity.getStatus(),
            entity.getTitle(),
            entity.getCity(),
            entity.getSalary(),
            entity.getExperience(),
            entity.getAutomationLevel(),
            entity.getStrategyJson(),
            entity.getCreatedAt()
        );
    }

    private String toJson(Object value) {
        if (value == null) {
            return "[]";
        }
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private String buildRuleConfigJson(String title,
                                       String city,
                                       String salary,
                                       String experience,
                                       String automationLevel,
                                       List<String> exclude,
                                       List<String> preferences) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (title != null) {
            payload.put("title", title);
        }
        if (city != null) {
            payload.put("city", city);
        }
        if (salary != null) {
            payload.put("salary", salary);
        }
        if (experience != null) {
            payload.put("experience", experience);
        }
        if (automationLevel != null) {
            payload.put("automationLevel", automationLevel);
        }
        if (!exclude.isEmpty()) {
            payload.put("exclude", exclude);
        }
        if (!preferences.isEmpty()) {
            payload.put("preferences", preferences);
        }
        try {
            return mapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private boolean isAllowedStatus(String status) {
        return "PAUSED".equals(status)
            || "COMPLETED".equals(status)
            || "FAILED".equals(status);
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return null;
    }

    private List<String> firstNonEmptyList(List<String> primary, List<String> fallback) {
        if (primary != null && !primary.isEmpty()) {
            return primary;
        }
        if (fallback != null && !fallback.isEmpty()) {
            return fallback;
        }
        return List.of();
    }

    private boolean isBlankJsonArray(String json) {
        return json == null || json.isBlank() || "[]".equals(json.trim());
    }

    private List<String> parseJsonList(String json) {
        if (isBlankJsonArray(json)) {
            return List.of();
        }
        try {
            return mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception ex) {
            return List.of();
        }
    }
}
