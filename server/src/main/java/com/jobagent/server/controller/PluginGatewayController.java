package com.jobagent.server.controller;

import com.jobagent.server.dto.ActionReportRequest;
import com.jobagent.server.dto.ChatReportRequest;
import com.jobagent.server.dto.ChatReportResponse;
import com.jobagent.server.dto.HeartbeatRequest;
import com.jobagent.server.dto.PageReportRequest;
import com.jobagent.server.dto.PageReportResponse;
import com.jobagent.server.dto.StatusResponse;
import com.jobagent.server.service.AuthService;
import com.jobagent.server.service.ConversationService;
import com.jobagent.server.service.JobPostService;
import com.jobagent.server.service.AuditService;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/plugin")
public class PluginGatewayController {

    private final JobPostService jobPostService;
    private final ConversationService conversationService;
    private final AuthService authService;
    private final AuditService auditService;

    public PluginGatewayController(JobPostService jobPostService,
                                   ConversationService conversationService,
                                   AuthService authService,
                                   AuditService auditService) {
        this.jobPostService = jobPostService;
        this.conversationService = conversationService;
        this.authService = authService;
        this.auditService = auditService;
    }

    @PostMapping("/page/report")
    public PageReportResponse pageReport(@RequestHeader("X-Plugin-Token") String pluginToken,
                                         @Valid @RequestBody PageReportRequest request) {
        String userId = authService.requireUserIdFromPluginToken(pluginToken);
        auditService.record(userId, "PLUGIN_PAGE_REPORT",
            "{\"task_id\":\"" + request.taskId() + "\"}");
        try {
            return jobPostService.handlePageReport(request, userId);
        } catch (ValidationException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "WORKER_TIMEOUT");
        }
    }

    @PostMapping("/chat/report")
    public ChatReportResponse chatReport(@RequestHeader("X-Plugin-Token") String pluginToken,
                                         @Valid @RequestBody ChatReportRequest request) {
        String userId = authService.requireUserIdFromPluginToken(pluginToken);
        auditService.record(userId, "PLUGIN_CHAT_REPORT",
            "{\"task_id\":\"" + request.taskId() + "\"}");
        try {
            return conversationService.handleChatReport(request, userId);
        } catch (ValidationException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "WORKER_TIMEOUT");
        }
    }

    @PostMapping("/action/report")
    public StatusResponse actionReport(@RequestHeader("X-Plugin-Token") String pluginToken,
                                       @Valid @RequestBody ActionReportRequest request) {
        String userId = authService.requireUserIdFromPluginToken(pluginToken);
        auditService.record(userId, "PLUGIN_ACTION",
            "{\"task_id\":\"" + request.taskId() + "\"}");
        conversationService.handleActionReport(request, userId);
        return new StatusResponse("ok");
    }

    @PostMapping("/heartbeat")
    public StatusResponse heartbeat(@RequestHeader("X-Plugin-Token") String pluginToken,
                                    @Valid @RequestBody HeartbeatRequest request) {
        authService.verifyPluginToken(pluginToken);
        return new StatusResponse("ok");
    }
}
