package com.jobagent.server.controller;

import com.jobagent.server.dto.TaskCreateRequest;
import com.jobagent.server.dto.TaskResponse;
import com.jobagent.server.store.TaskStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskStore store;

    public TaskController(TaskStore store) {
        this.store = store;
    }

    @PostMapping
    public TaskResponse create(@RequestBody TaskCreateRequest request) {
        return store.create(request);
    }

    @GetMapping
    public List<TaskResponse> list() {
        return store.list();
    }
}
