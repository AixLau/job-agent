package com.jobagent.server.repository;

import com.jobagent.server.store.DashboardRecommendationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DashboardRecommendationRepository extends JpaRepository<DashboardRecommendationEntity, String> {

    List<DashboardRecommendationEntity> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
}
