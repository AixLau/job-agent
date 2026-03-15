package com.jobagent.server.repository;

import com.jobagent.server.store.JobMatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JobMatchRepository extends JpaRepository<JobMatchEntity, String> {
    Optional<JobMatchEntity> findByTaskIdAndJobPostId(String taskId, String jobPostId);
    List<JobMatchEntity> findAllByJobPostId(String jobPostId);
    long deleteByCreatedAtBefore(Instant cutoff);
}
