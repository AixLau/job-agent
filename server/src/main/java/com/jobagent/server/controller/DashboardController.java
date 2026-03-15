package com.jobagent.server.controller;

import com.jobagent.server.dto.DashboardResponse;
import com.jobagent.server.service.AuthService;
import com.jobagent.server.store.DashboardStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardStore store;
    private final AuthService authService;

    public DashboardController(DashboardStore store, AuthService authService) {
        this.store = store;
        this.authService = authService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String userId = authService.requireUserId(authorization);
        return store.snapshot(userId);
    }
}
