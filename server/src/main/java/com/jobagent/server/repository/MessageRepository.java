package com.jobagent.server.repository;

import com.jobagent.server.store.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageRepository extends JpaRepository<MessageEntity, String> {
    Optional<MessageEntity> findByConversationIdAndExternalId(String conversationId, String externalId);
}
