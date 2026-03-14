package com.jobagent.server.repository;

import com.jobagent.server.store.DashboardDraftEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DashboardDraftRepository extends JpaRepository<DashboardDraftEntity, String> {

    List<DashboardDraftEntity> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
}
