package com.jobagent.server.repository;

import com.jobagent.server.store.UserSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSettingsRepository extends JpaRepository<UserSettingsEntity, String> {
}
