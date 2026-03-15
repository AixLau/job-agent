package com.jobagent.server.controller;

import com.jobagent.server.dto.SettingsRequest;
import com.jobagent.server.dto.SettingsResponse;
import com.jobagent.server.repository.UserSettingsRepository;
import com.jobagent.server.service.AuthService;
import com.jobagent.server.store.UserSettingsEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final AuthService authService;
    private final UserSettingsRepository repository;

    public SettingsController(AuthService authService, UserSettingsRepository repository) {
        this.authService = authService;
        this.repository = repository;
    }

    @GetMapping
    public Map<String, SettingsResponse> get(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String userId = authService.requireUserId(authorization);
        return Map.of("settings", toResponse(loadOrCreate(userId)));
    }

    @PostMapping
    public Map<String, SettingsResponse> save(@RequestHeader(value = "Authorization", required = false) String authorization,
                                              @RequestBody SettingsRequest request) {
        String userId = authService.requireUserId(authorization);
        UserSettingsEntity entity = loadOrCreate(userId);
        if (request.defaultAutomationLevel() != null) {
            entity.setDefaultAutomationLevel(request.defaultAutomationLevel());
        }
        if (request.autoSendEnabled() != null) {
            entity.setAutoSendEnabled(request.autoSendEnabled());
        }
        if (request.highRiskRequiresReview() != null) {
            entity.setHighRiskRequiresReview(request.highRiskRequiresReview());
        }
        if (request.chatImmediateAutoSend() != null) {
            entity.setChatImmediateAutoSend(request.chatImmediateAutoSend());
        }
        if (request.dailyActionLimit() != null) {
            entity.setDailyActionLimit(request.dailyActionLimit());
        }
        entity.touch();
        repository.save(entity);
        return Map.of("settings", toResponse(entity));
    }

    private UserSettingsEntity loadOrCreate(String userId) {
        return repository.findById(userId).orElseGet(() -> repository.save(new UserSettingsEntity(
            userId,
            "SEMI",
            false,
            true,
            false,
            30,
            Instant.now()
        )));
    }

    private SettingsResponse toResponse(UserSettingsEntity entity) {
        return new SettingsResponse(
            entity.getDefaultAutomationLevel(),
            entity.isAutoSendEnabled(),
            entity.isHighRiskRequiresReview(),
            entity.isChatImmediateAutoSend(),
            entity.getDailyActionLimit()
        );
    }
}
