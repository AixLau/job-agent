package com.jobagent.server.controller;

import com.jobagent.server.dto.ResumeRequest;
import com.jobagent.server.dto.ResumeResponse;
import com.jobagent.server.store.ResumeStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    private final ResumeStore store;

    public ResumeController(ResumeStore store) {
        this.store = store;
    }

    @PostMapping
    public ResumeResponse upload(@RequestBody ResumeRequest request) {
        return store.save(request);
    }

    @GetMapping
    public ResumeResponse fetch() {
        return store.latest();
    }
}
