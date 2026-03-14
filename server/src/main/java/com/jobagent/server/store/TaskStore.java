package com.jobagent.server.store;

import com.jobagent.server.dto.TaskCreateRequest;
import com.jobagent.server.dto.TaskResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TaskStore {

    private final Map<String, TaskResponse> tasks = new ConcurrentHashMap<>();

    public TaskResponse create(TaskCreateRequest request) {
        String id = UUID.randomUUID().toString();
        TaskResponse response = new TaskResponse(
            id,
            "ACTIVE",
            request.targetRole(),
            request.city(),
            request.salary(),
            request.experience(),
            request.automationLevel()
        );
        tasks.put(id, response);
        return response;
    }

    public List<TaskResponse> list() {
        return new ArrayList<>(tasks.values());
    }
}
