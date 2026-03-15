package com.jobagent.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.server.dto.DraftApproveRequest;
import com.jobagent.server.dto.DraftApproveResponse;
import com.jobagent.server.dto.DraftRejectRequest;
import com.jobagent.server.dto.DraftRejectResponse;
import com.jobagent.server.service.AuditService;
import com.jobagent.server.service.AuthService;
import com.jobagent.server.service.ConversationService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/drafts")
public class DraftController {

    private final ConversationService conversationService;
    private final AuthService authService;
    private final AuditService auditService;
    private final ObjectMapper mapper;

    public DraftController(ConversationService conversationService,
                           AuthService authService,
                           AuditService auditService,
                           ObjectMapper mapper) {
        this.conversationService = conversationService;
        this.authService = authService;
        this.auditService = auditService;
        this.mapper = mapper;
    }

    @PostMapping("/{id}/approve")
    public DraftApproveResponse approve(@PathVariable("id") String draftId,
                                        @RequestHeader(value = "Authorization", required = false) String authorization,
                                        @RequestBody(required = false) DraftApproveRequest request) {
        String userId = authService.requireUserId(authorization);
        String action = request == null ? null : request.action();
        auditService.record(userId, "DRAFT_APPROVE", toJson(Map.of(
            "draft_id", draftId,
            "action", action
        )));
        return conversationService.approveDraft(draftId, userId);
    }

    @PostMapping("/{id}/reject")
    public DraftRejectResponse reject(@PathVariable("id") String draftId,
                                      @RequestHeader(value = "Authorization", required = false) String authorization,
                                      @RequestBody(required = false) DraftRejectRequest request) {
        String userId = authService.requireUserId(authorization);
        String reason = request == null ? null : request.reason();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("draft_id", draftId);
        if (reason != null) {
            payload.put("reason", reason);
        }
        auditService.record(userId, "DRAFT_REJECT", toJson(payload));
        return conversationService.rejectDraft(draftId, userId);
    }

    private String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }
}
