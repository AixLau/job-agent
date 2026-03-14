package com.jobagent.server.controller;

import com.jobagent.server.dto.DashboardResponse;
import com.jobagent.server.store.DashboardStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardStore store;

    public DashboardController(DashboardStore store) {
        this.store = store;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return store.snapshot();
    }
}
