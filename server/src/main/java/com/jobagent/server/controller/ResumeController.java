package com.jobagent.server.controller;

import com.jobagent.server.dto.ResumeConfirmRequest;
import com.jobagent.server.dto.ResumeParseResponse;
import com.jobagent.server.dto.ResumeRequest;
import com.jobagent.server.dto.ResumeResponse;
import com.jobagent.server.service.AuditService;
import com.jobagent.server.service.AuthService;
import com.jobagent.server.service.ResumeParseService;
import com.jobagent.server.store.ResumeStore;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    private final ResumeStore store;
    private final AuthService authService;
    private final AuditService auditService;
    private final ResumeParseService resumeParseService;

    public ResumeController(ResumeStore store,
                            AuthService authService,
                            AuditService auditService,
                            ResumeParseService resumeParseService) {
        this.store = store;
        this.authService = authService;
        this.auditService = auditService;
        this.resumeParseService = resumeParseService;
    }

    @PostMapping
    public ResumeResponse upload(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @Valid @RequestBody ResumeRequest request) {
        String userId = authService.requireUserId(authorization);
        ResumeResponse response = store.save(request, userId);
        auditService.record(userId, "RESUME_UPLOAD",
            "{\"resume_id\":\"" + response.resume().id() + "\"}");
        return response;
    }

    @PostMapping("/parse")
    public ResumeParseResponse parse(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @Valid @RequestBody ResumeRequest request) {
        authService.requireUserId(authorization);
        return new ResumeParseResponse(resumeParseService.parse(request));
    }

    @PostMapping("/confirm")
    public ResumeResponse confirm(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @Valid @RequestBody ResumeConfirmRequest request) {
        String userId = authService.requireUserId(authorization);
        ResumeResponse response = store.saveParsed(
            userId,
            request.content(),
            request.parsedJson() == null ? Map.of() : request.parsedJson()
        );
        auditService.record(userId, "RESUME_CONFIRM",
            "{\"resume_id\":\"" + response.resume().id() + "\"}");
        return response;
    }

    @GetMapping
    public ResumeResponse fetch(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String userId = authService.requireUserId(authorization);
        return store.latest(userId);
    }
}
