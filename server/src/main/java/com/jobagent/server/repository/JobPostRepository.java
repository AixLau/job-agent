package com.jobagent.server.repository;

import com.jobagent.server.store.JobPostEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobPostRepository extends JpaRepository<JobPostEntity, String> {
    Optional<JobPostEntity> findBySourceAndExternalId(String source, String externalId);
}
