package com.jobagent.server.service;

import com.jobagent.server.repository.AuditLogRepository;
import com.jobagent.server.repository.JobMatchRepository;
import com.jobagent.server.repository.JobPostRepository;
import com.jobagent.server.repository.MessageDraftRepository;
import com.jobagent.server.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class RetentionService {

    private final JobPostRepository jobPostRepository;
    private final JobMatchRepository jobMatchRepository;
    private final MessageRepository messageRepository;
    private final MessageDraftRepository messageDraftRepository;
    private final AuditLogRepository auditLogRepository;
    private final int retentionDays;

    public RetentionService(JobPostRepository jobPostRepository,
                            JobMatchRepository jobMatchRepository,
                            MessageRepository messageRepository,
                            MessageDraftRepository messageDraftRepository,
                            AuditLogRepository auditLogRepository,
                            @Value("${job-agent.retention.days:90}") int retentionDays) {
        this.jobPostRepository = jobPostRepository;
        this.jobMatchRepository = jobMatchRepository;
        this.messageRepository = messageRepository;
        this.messageDraftRepository = messageDraftRepository;
        this.auditLogRepository = auditLogRepository;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${job-agent.retention.cron:0 0 3 * * *}")
    @Transactional
    public void purgeOldData() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        messageDraftRepository.deleteByCreatedAtBefore(cutoff);
        messageRepository.deleteByCreatedAtBefore(cutoff);
        jobMatchRepository.deleteByCreatedAtBefore(cutoff);
        jobPostRepository.deleteByCreatedAtBefore(cutoff);
        auditLogRepository.deleteByCreatedAtBefore(cutoff);
    }
}
