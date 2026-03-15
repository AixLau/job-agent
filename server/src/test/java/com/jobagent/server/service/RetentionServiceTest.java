package com.jobagent.server.service;

import com.jobagent.server.JobAgentServerApplication;
import com.jobagent.server.repository.AuditLogRepository;
import com.jobagent.server.repository.JobMatchRepository;
import com.jobagent.server.repository.JobPostRepository;
import com.jobagent.server.repository.MessageDraftRepository;
import com.jobagent.server.repository.MessageRepository;
import com.jobagent.server.repository.UserCompanyBlacklistRepository;
import com.jobagent.server.repository.UserJobActionRepository;
import com.jobagent.server.store.AuditLogEntity;
import com.jobagent.server.store.JobMatchEntity;
import com.jobagent.server.store.JobPostEntity;
import com.jobagent.server.store.MessageDraftEntity;
import com.jobagent.server.store.MessageEntity;
import com.jobagent.server.store.UserCompanyBlacklistEntity;
import com.jobagent.server.store.UserJobActionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = JobAgentServerApplication.class)
class RetentionServiceTest {

    @Autowired
    private RetentionService retentionService;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private JobMatchRepository jobMatchRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageDraftRepository messageDraftRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserJobActionRepository userJobActionRepository;

    @Autowired
    private UserCompanyBlacklistRepository userCompanyBlacklistRepository;

    @BeforeEach
    void setup() {
        userCompanyBlacklistRepository.deleteAll();
        userJobActionRepository.deleteAll();
        auditLogRepository.deleteAll();
        messageDraftRepository.deleteAll();
        messageRepository.deleteAll();
        jobMatchRepository.deleteAll();
        jobPostRepository.deleteAll();
    }

    @Test
    void retention_deletes_records_older_than_90_days() {
        Instant old = Instant.now().minus(120, ChronoUnit.DAYS);
        jobPostRepository.save(new JobPostEntity("job-1", "task-1", "boss", "ext-1", "Role", "Company", "Shanghai", "20k", "3y", "jd", "{}", "ACTIVE", old));
        jobMatchRepository.save(new JobMatchEntity("match-1", "task-1", "job-1", 80, "[]", "[]", "{}", old));
        messageRepository.save(new MessageEntity("msg-1", "conv-1", "hr", "hello", "ext-msg-1", old));
        messageDraftRepository.save(new MessageDraftEntity("draft-1", "conv-1", "draft", "AI", false, old));
        auditLogRepository.save(new AuditLogEntity("audit-1", "user-1", "TASK_CREATE", "{}", old));

        retentionService.purgeOldData();

        assertThat(jobPostRepository.findAll()).isEmpty();
        assertThat(jobMatchRepository.findAll()).isEmpty();
        assertThat(messageRepository.findAll()).isEmpty();
        assertThat(messageDraftRepository.findAll()).isEmpty();
        assertThat(auditLogRepository.findAll()).isEmpty();
    }

    @Test
    void retention_keeps_user_preferences() {
        Instant old = Instant.now().minus(120, ChronoUnit.DAYS);
        userJobActionRepository.save(new UserJobActionEntity(
            UUID.randomUUID().toString(),
            "user-1",
            "job-1",
            "boss",
            "FOLLOW",
            old
        ));
        userCompanyBlacklistRepository.save(new UserCompanyBlacklistEntity(
            UUID.randomUUID().toString(),
            "user-1",
            "Bad Company",
            "boss",
            old
        ));

        retentionService.purgeOldData();

        assertThat(userJobActionRepository.findAll()).isNotEmpty();
        assertThat(userCompanyBlacklistRepository.findAll()).isNotEmpty();
    }
}
