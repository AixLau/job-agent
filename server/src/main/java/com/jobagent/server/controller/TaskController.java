package com.jobagent.server.controller;

import com.jobagent.server.dto.TaskCreateRequest;
import com.jobagent.server.dto.TaskCreateResponse;
import com.jobagent.server.dto.TaskListResponse;
import com.jobagent.server.dto.TaskUpdateRequest;
import com.jobagent.server.service.AuditService;
import com.jobagent.server.service.AuthService;
import com.jobagent.server.store.TaskStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskStore store;
    private final AuthService authService;
    private final AuditService auditService;

    public TaskController(TaskStore store, AuthService authService, AuditService auditService) {
        this.store = store;
        this.authService = authService;
        this.auditService = auditService;
    }

    @PostMapping
    public TaskCreateResponse create(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @RequestBody TaskCreateRequest request) {
        String userId = authService.requireUserId(authorization);
        TaskCreateResponse response = new TaskCreateResponse(store.create(request, userId));
        auditService.record(userId, "TASK_CREATE", "{\"task_id\":\"" + response.task().id() + "\"}");
        return response;
    }

    @GetMapping
    public TaskListResponse list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String userId = authService.requireUserId(authorization);
        return new TaskListResponse(store.listForUser(userId));
    }

    @PatchMapping("/{taskId}")
    public TaskCreateResponse update(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable("taskId") String taskId,
                                     @RequestBody TaskUpdateRequest request) {
        String userId = authService.requireUserId(authorization);
        TaskCreateResponse response = new TaskCreateResponse(store.update(taskId, userId, request));
        auditService.record(userId, "TASK_UPDATE", "{\"task_id\":\"" + response.task().id() + "\"}");
        return response;
    }
}
