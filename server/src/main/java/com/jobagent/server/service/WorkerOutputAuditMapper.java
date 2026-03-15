package com.jobagent.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.server.dto.ActionReportRequest;
import com.jobagent.server.dto.ChatReportRequest;
import com.jobagent.server.dto.ChatReportResponse;
import com.jobagent.server.dto.PageReportRequest;
import com.jobagent.server.dto.PageReportResponse;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkerOutputAuditMapper {

    private final ObjectMapper objectMapper;

    public WorkerOutputAuditMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuditRecord pageSuccess(PageReportRequest request, PageReportResponse response) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("task_id", request.taskId());
        payload.put("page_type", request.pageType());
        payload.put("source_url", request.sourceUrl());
        payload.put("want_draft", request.wantDraft());
        payload.put("extracted_json", request.extractedJson());
        Map<String, Object> modelOutput = new LinkedHashMap<>();
        modelOutput.put("analysis", response.analysis());
        modelOutput.put("draft", response.draft());
        return new AuditRecord(
            writeJson(payload),
            "OK",
            writeJson(modelOutput),
            response.analysis() == null || response.analysis().riskTags() == null
                ? List.of()
                : response.analysis().riskTags()
        );
    }

    public AuditRecord chatSuccess(ChatReportRequest request, ChatReportResponse response) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("task_id", request.taskId());
        payload.put("conversation_id", request.conversationId());
        payload.put("last_message_id", request.lastMessageId());
        payload.put("message_count", request.messages() == null ? 0 : request.messages().size());
        Map<String, Object> modelOutput = new LinkedHashMap<>();
        modelOutput.put("reply", response.reply());
        modelOutput.put("auto_send", response.autoSend());
        modelOutput.put("draft", response.draft());
        modelOutput.put("action_hint", response.actionHint());
        return new AuditRecord(
            writeJson(payload),
            "OK",
            writeJson(modelOutput),
            List.of()
        );
    }

    public AuditRecord actionResult(ActionReportRequest request, String result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("task_id", request.taskId());
        payload.put("action_type", request.actionType());
        payload.put("status", request.status());
        payload.put("payload", request.payload());
        return new AuditRecord(writeJson(payload), result, null, List.of());
    }

    public String failurePayload(Map<String, Object> payload) {
        return writeJson(payload);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    public record AuditRecord(String payload, String result, String modelOutput, List<String> riskTags) {
    }
}
