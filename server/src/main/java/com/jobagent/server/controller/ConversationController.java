package com.jobagent.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.server.dto.ConversationCloseRequest;
import com.jobagent.server.dto.ConversationCloseResponse;
import com.jobagent.server.dto.ConversationDetailResponse;
import com.jobagent.server.dto.DraftRegenerateRequest;
import com.jobagent.server.dto.DraftRegenerateResponse;
import com.jobagent.server.service.AuditService;
import com.jobagent.server.service.AuthService;
import com.jobagent.server.service.ConversationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final AuthService authService;
    private final AuditService auditService;
    private final ObjectMapper mapper;

    public ConversationController(ConversationService conversationService,
                                  AuthService authService,
                                  AuditService auditService,
                                  ObjectMapper mapper) {
        this.conversationService = conversationService;
        this.authService = authService;
        this.auditService = auditService;
        this.mapper = mapper;
    }

    @GetMapping("/{id}")
    public ConversationDetailResponse detail(@PathVariable("id") String conversationId,
                                             @RequestHeader(value = "Authorization", required = false) String authorization) {
        String userId = authService.requireUserId(authorization);
        return conversationService.detail(conversationId, userId);
    }

    @PostMapping("/{id}/close")
    public ConversationCloseResponse close(@PathVariable("id") String conversationId,
                                           @RequestHeader(value = "Authorization", required = false) String authorization,
                                           @RequestBody(required = false) ConversationCloseRequest request) {
        String userId = authService.requireUserId(authorization);
        String reason = request == null ? null : request.reason();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("conversation_id", conversationId);
        if (reason != null) {
            payload.put("reason", reason);
        }
        auditService.record(userId, "CONVERSATION_CLOSE", toJson(payload));
        return conversationService.close(conversationId, userId);
    }

    @PostMapping("/{id}/regenerate")
    public DraftRegenerateResponse regenerate(@PathVariable("id") String conversationId,
                                              @RequestHeader(value = "Authorization", required = false) String authorization,
                                              @RequestBody(required = false) DraftRegenerateRequest request) {
        String userId = authService.requireUserId(authorization);
        String style = request == null ? null : request.style();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("conversation_id", conversationId);
        if (style != null) {
            payload.put("style", style);
        }
        auditService.record(userId, "DRAFT_REGENERATE", toJson(payload));
        return conversationService.regenerate(conversationId, userId);
    }

    private String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }
}
