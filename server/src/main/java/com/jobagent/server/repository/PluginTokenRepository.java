package com.jobagent.server.repository;

import com.jobagent.server.store.PluginTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PluginTokenRepository extends JpaRepository<PluginTokenEntity, String> {
    Optional<PluginTokenEntity> findByToken(String token);
}
