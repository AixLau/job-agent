package com.jobagent.server.service;

import com.jobagent.server.repository.AuditLogRepository;
import com.jobagent.server.store.AuditLogEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void record(String userId, String actionType, String payload) {
        repository.save(new AuditLogEntity(
            UUID.randomUUID().toString(),
            userId,
            actionType,
            payload
        ));
    }
}
