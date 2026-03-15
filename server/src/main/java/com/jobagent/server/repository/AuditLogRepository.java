package com.jobagent.server.repository;

import com.jobagent.server.store.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, String> {
    Page<AuditLogEntity> findAllByUserIdOrderByCreatedAtDescIdDesc(String userId, Pageable pageable);
}
