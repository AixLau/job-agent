package com.jobagent.server.repository;

import com.jobagent.server.store.DashboardReplyEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DashboardReplyRepository extends JpaRepository<DashboardReplyEntity, String> {

    List<DashboardReplyEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
