package com.jobagent.server.controller;

import com.jobagent.server.dto.ProfileRequest;
import com.jobagent.server.dto.ProfileResponse;
import com.jobagent.server.service.AuthService;
import com.jobagent.server.service.ProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final AuthService authService;
    private final ProfileService profileService;

    public ProfileController(AuthService authService, ProfileService profileService) {
        this.authService = authService;
        this.profileService = profileService;
    }

    @GetMapping
    public ProfileResponse fetch(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String userId = authService.requireUserId(authorization);
        return profileService.fetch(userId);
    }

    @PostMapping
    public ProfileResponse save(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @RequestBody ProfileRequest request) {
        String userId = authService.requireUserId(authorization);
        return profileService.save(userId, request);
    }
}
