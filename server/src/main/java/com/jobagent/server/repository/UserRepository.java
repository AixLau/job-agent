package com.jobagent.server.repository;

import com.jobagent.server.store.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByAccount(String account);
}
