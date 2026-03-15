package com.jobagent.server.repository;

import com.jobagent.server.store.ConversationEntity;
import com.jobagent.server.store.JobPostEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class JobDomainRepositoryTest {

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Test
    void job_post_unique_source_external_id() {
        JobPostEntity first = new JobPostEntity(
            "jp-1",
            "task-1",
            "boss",
            "ext-1",
            "Role A",
            "Company A",
            "Shanghai",
            "20k-30k",
            "3y",
            "jd",
            "{}",
            "DISCOVERED",
            null
        );
        JobPostEntity dup = new JobPostEntity(
            "jp-2",
            "task-2",
            "boss",
            "ext-1",
            "Role B",
            "Company B",
            "Beijing",
            "30k-40k",
            "5y",
            "jd",
            "{}",
            "DISCOVERED",
            null
        );

        jobPostRepository.saveAndFlush(first);
        assertThatThrownBy(() -> jobPostRepository.saveAndFlush(dup))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void conversation_unique_task_external_id() {
        ConversationEntity first = new ConversationEntity(
            "c-1",
            "task-1",
            "jp-1",
            "conv-1",
            "NEW",
            null,
            null,
            null,
            null
        );
        ConversationEntity dup = new ConversationEntity(
            "c-2",
            "task-1",
            "jp-2",
            "conv-1",
            "NEW",
            null,
            null,
            null,
            null
        );

        conversationRepository.saveAndFlush(first);
        assertThatThrownBy(() -> conversationRepository.saveAndFlush(dup))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
