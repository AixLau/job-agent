package com.jobagent.server.controller;

import com.jobagent.server.dto.DashboardResponse;
import com.jobagent.server.dto.InterviewListResponse;
import com.jobagent.server.dto.RecommendationListResponse;
import com.jobagent.server.dto.ReplyListResponse;
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

    @GetMapping("/recommendations")
    public RecommendationListResponse recommendations(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String userId = authService.requireUserId(authorization);
        return new RecommendationListResponse(store.recommendations(userId));
    }

    @GetMapping("/replies")
    public ReplyListResponse replies(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String userId = authService.requireUserId(authorization);
        return new ReplyListResponse(store.replies(userId));
    }

    @GetMapping("/interviews")
    public InterviewListResponse interviews(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String userId = authService.requireUserId(authorization);
        return new InterviewListResponse(store.interviews(userId));
    }
}
