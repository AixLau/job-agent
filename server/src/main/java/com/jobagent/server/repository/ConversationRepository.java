package com.jobagent.server.repository;

import com.jobagent.server.store.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<ConversationEntity, String> {
    Optional<ConversationEntity> findByTaskIdAndExternalId(String taskId, String externalId);
}
