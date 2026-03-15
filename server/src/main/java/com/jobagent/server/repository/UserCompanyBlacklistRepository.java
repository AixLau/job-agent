package com.jobagent.server.repository;

import com.jobagent.server.store.UserCompanyBlacklistEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCompanyBlacklistRepository extends JpaRepository<UserCompanyBlacklistEntity, String> {
    Optional<UserCompanyBlacklistEntity> findByUserIdAndCompanyNameAndSource(String userId, String companyName, String source);
}
