package com.jobagent.server.repository;

import com.jobagent.server.store.UserJobActionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserJobActionRepository extends JpaRepository<UserJobActionEntity, String> {
    Optional<UserJobActionEntity> findByUserIdAndJobPostId(String userId, String jobPostId);
    Page<UserJobActionEntity> findAllByUserIdAndActionTypeOrderByCreatedAtDescIdDesc(String userId, String actionType, Pageable pageable);
}
