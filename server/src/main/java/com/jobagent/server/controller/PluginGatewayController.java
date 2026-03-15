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
import com.jobagent.server.service.WorkerOutputAuditMapper;
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
    private final WorkerOutputAuditMapper workerOutputAuditMapper;

    public PluginGatewayController(JobPostService jobPostService,
                                   ConversationService conversationService,
                                   AuthService authService,
                                   AuditService auditService,
                                   WorkerOutputAuditMapper workerOutputAuditMapper) {
        this.jobPostService = jobPostService;
        this.conversationService = conversationService;
        this.authService = authService;
        this.auditService = auditService;
        this.workerOutputAuditMapper = workerOutputAuditMapper;
    }

    @PostMapping("/page/report")
    public PageReportResponse pageReport(@RequestHeader("X-Plugin-Token") String pluginToken,
                                         @Valid @RequestBody PageReportRequest request) {
        String userId = authService.requireUserIdFromPluginToken(pluginToken);
        try {
            PageReportResponse response = jobPostService.handlePageReport(request, userId);
            WorkerOutputAuditMapper.AuditRecord record = workerOutputAuditMapper.pageSuccess(request, response);
            auditService.record(userId, "PLUGIN_PAGE_REPORT", record.payload(), record.result(), record.modelOutput(), record.riskTags());
            return response;
        } catch (ValidationException ex) {
            auditService.record(
                userId,
                "PLUGIN_PAGE_REPORT",
                workerOutputAuditMapper.failurePayload(java.util.Map.of("task_id", request.taskId())),
                "VALIDATION_FAILED",
                null,
                java.util.List.of()
            );
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
        } catch (RestClientException ex) {
            auditService.record(
                userId,
                "PLUGIN_PAGE_REPORT",
                workerOutputAuditMapper.failurePayload(java.util.Map.of("task_id", request.taskId())),
                "WORKER_TIMEOUT",
                null,
                java.util.List.of()
            );
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "WORKER_TIMEOUT");
        }
    }

    @PostMapping("/chat/report")
    public ChatReportResponse chatReport(@RequestHeader("X-Plugin-Token") String pluginToken,
                                         @Valid @RequestBody ChatReportRequest request) {
        String userId = authService.requireUserIdFromPluginToken(pluginToken);
        try {
            ChatReportResponse response = conversationService.handleChatReport(request, userId);
            WorkerOutputAuditMapper.AuditRecord record = workerOutputAuditMapper.chatSuccess(request, response);
            auditService.record(userId, "PLUGIN_CHAT_REPORT", record.payload(), record.result(), record.modelOutput(), record.riskTags());
            return response;
        } catch (ValidationException ex) {
            auditService.record(
                userId,
                "PLUGIN_CHAT_REPORT",
                workerOutputAuditMapper.failurePayload(java.util.Map.of("task_id", request.taskId())),
                "VALIDATION_FAILED",
                null,
                java.util.List.of()
            );
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
        } catch (RestClientException ex) {
            auditService.record(
                userId,
                "PLUGIN_CHAT_REPORT",
                workerOutputAuditMapper.failurePayload(java.util.Map.of("task_id", request.taskId())),
                "WORKER_TIMEOUT",
                null,
                java.util.List.of()
            );
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "WORKER_TIMEOUT");
        }
    }

    @PostMapping("/action/report")
    public StatusResponse actionReport(@RequestHeader("X-Plugin-Token") String pluginToken,
                                       @Valid @RequestBody ActionReportRequest request) {
        String userId = authService.requireUserIdFromPluginToken(pluginToken);
        conversationService.handleActionReport(request, userId);
        WorkerOutputAuditMapper.AuditRecord record = workerOutputAuditMapper.actionResult(request, "OK");
        auditService.record(userId, "PLUGIN_ACTION", record.payload(), record.result(), record.modelOutput(), record.riskTags());
        return new StatusResponse("ok");
    }

    @PostMapping("/heartbeat")
    public StatusResponse heartbeat(@RequestHeader("X-Plugin-Token") String pluginToken,
                                    @Valid @RequestBody HeartbeatRequest request) {
        authService.verifyPluginToken(pluginToken);
        return new StatusResponse("ok");
    }
}
