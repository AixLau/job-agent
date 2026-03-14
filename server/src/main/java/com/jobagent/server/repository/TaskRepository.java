package com.jobagent.server.repository;

import com.jobagent.server.store.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<TaskEntity, String> {
}
