package com.jobagent.server.repository;

import com.jobagent.server.store.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<TaskEntity, String> {
    Optional<TaskEntity> findByIdAndUserId(String id, String userId);
    List<TaskEntity> findAllByUserId(String userId);
}
