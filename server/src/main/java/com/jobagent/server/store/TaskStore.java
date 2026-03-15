package com.jobagent.server.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.server.dto.TaskCreateRequest;
import com.jobagent.server.dto.TaskResponse;
import com.jobagent.server.dto.TaskUpdateRequest;
import com.jobagent.server.repository.TaskRepository;
import com.jobagent.server.service.StrategyService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class TaskStore {

    private final TaskRepository repository;
    private final StrategyService strategyService;
    private final ObjectMapper mapper;

    public TaskStore(TaskRepository repository, StrategyService strategyService, ObjectMapper mapper) {
        this.repository = repository;
        this.strategyService = strategyService;
        this.mapper = mapper;
    }

    public TaskResponse create(TaskCreateRequest request, String userId) {
        String id = UUID.randomUUID().toString();
        String strategyJson = strategyService.parse(request.strategyText(), id);
        TaskEntity entity = new TaskEntity(
            id,
            userId,
            request.title(),
            request.city(),
            request.salary(),
            request.experience(),
            request.automationLevel(),
            "ACTIVE",
            strategyJson,
            toJson(request.exclude()),
            toJson(request.preferences()),
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
        if (request.automationLevel() != null) {
            entity.setAutomationLevel(request.automationLevel());
        }
        if (request.exclude() != null) {
            entity.setExcludeJson(toJson(request.exclude()));
        }
        if (request.preferences() != null) {
            entity.setPreferencesJson(toJson(request.preferences()));
        }
        if (request.strategyText() != null) {
            entity.setStrategyJson(strategyService.parse(request.strategyText(), entity.getId()));
        }
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
}
