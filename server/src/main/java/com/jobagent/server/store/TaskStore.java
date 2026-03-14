package com.jobagent.server.store;

import com.jobagent.server.dto.TaskCreateRequest;
import com.jobagent.server.dto.TaskResponse;
import com.jobagent.server.repository.TaskRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class TaskStore {

    private final TaskRepository repository;

    public TaskStore(TaskRepository repository) {
        this.repository = repository;
    }

    public TaskResponse create(TaskCreateRequest request) {
        String id = UUID.randomUUID().toString();
        TaskEntity entity = new TaskEntity(
            id,
            request.targetRole(),
            request.city(),
            request.salary(),
            request.experience(),
            request.automationLevel(),
            "ACTIVE"
        );
        TaskEntity saved = repository.save(entity);
        return toResponse(saved);
    }

    public List<TaskResponse> list() {
        return repository.findAll()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private TaskResponse toResponse(TaskEntity entity) {
        return new TaskResponse(
            entity.getId(),
            entity.getStatus(),
            entity.getTargetRole(),
            entity.getCity(),
            entity.getSalary(),
            entity.getExperience(),
            entity.getAutomationLevel()
        );
    }
}
