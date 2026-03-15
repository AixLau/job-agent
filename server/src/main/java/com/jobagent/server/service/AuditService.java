package com.jobagent.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.server.repository.AuditLogRepository;
import com.jobagent.server.store.AuditLogEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void record(String userId, String actionType, String payload) {
        record(userId, actionType, payload, "OK", null, List.of());
    }

    public void record(String userId,
                       String actionType,
                       String payload,
                       String result,
                       String modelOutput,
                       List<String> riskTags) {
        repository.save(new AuditLogEntity(
            UUID.randomUUID().toString(),
            userId,
            actionType,
            payload,
            result,
            modelOutput,
            writeRiskTags(riskTags)
        ));
    }

    private String writeRiskTags(List<String> riskTags) {
        try {
            return objectMapper.writeValueAsString(riskTags == null ? List.of() : riskTags);
        } catch (Exception ex) {
            return "[]";
        }
    }
}
