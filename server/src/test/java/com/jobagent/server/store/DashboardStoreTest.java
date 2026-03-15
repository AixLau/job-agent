package com.jobagent.server.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.server.dto.DraftItem;
import com.jobagent.server.dto.RecommendationItem;
import com.jobagent.server.dto.ReplyItem;
import com.jobagent.server.repository.DashboardDraftRepository;
import com.jobagent.server.repository.DashboardRecommendationRepository;
import com.jobagent.server.repository.DashboardReplyRepository;
import com.jobagent.server.repository.ConversationRepository;
import com.jobagent.server.repository.JobPostRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(DashboardStoreTest.TestConfig.class)
@TestPropertySource(properties = "job-agent.dashboard.max-items=1")
class DashboardStoreTest {

    @Autowired
    private DashboardStore store;

    @Autowired
    private DashboardRecommendationRepository recommendationRepository;

    @Autowired
    private DashboardDraftRepository draftRepository;

    @Autowired
    private DashboardReplyRepository replyRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Test
    void snapshotReturnsLatestAndMetrics() {
        store.addRecommendation("user-1", new RecommendationItem("job-1", "A", "C1", 80, List.of("r1"), "ACTIVE"));
        store.addDraft("user-1", new DraftItem("draft-1", "conv-1", "d1", Instant.now(), false));
        store.addReply("user-1", new ReplyItem("conv-1", "s1", "INTERVIEW", Instant.now()));

        assertThat(recommendationRepository.count()).isEqualTo(1);
        assertThat(draftRepository.count()).isEqualTo(1);
        assertThat(replyRepository.count()).isEqualTo(1);

        recommendationRepository.deleteAll();
        draftRepository.deleteAll();
        replyRepository.deleteAll();
        recommendationRepository.save(new DashboardRecommendationEntity(
            "rec-1",
            "user-1",
            "job-1",
            "A",
            "C1",
            80,
            "[\"r1\"]",
            "ACTIVE",
            Instant.parse("2024-01-01T00:00:00Z")
        ));
        recommendationRepository.save(new DashboardRecommendationEntity(
            "rec-2",
            "user-1",
            "job-2",
            "B",
            "C1",
            81,
            "[\"r2\"]",
            "ACTIVE",
            Instant.parse("2024-01-02T00:00:00Z")
        ));
        draftRepository.save(new DashboardDraftEntity(
            "draft-1",
            "user-1",
            "conv-1",
            "d1",
            false,
            Instant.parse("2024-01-01T00:00:00Z")
        ));
        draftRepository.save(new DashboardDraftEntity(
            "draft-2",
            "user-1",
            "conv-2",
            "d2",
            true,
            Instant.parse("2024-01-02T00:00:00Z")
        ));
        replyRepository.save(new DashboardReplyEntity(
            "reply-1",
            "user-1",
            "conv-1",
            "s1",
            "INTERVIEW",
            Instant.parse("2024-01-01T00:00:00Z")
        ));
        replyRepository.save(new DashboardReplyEntity(
            "reply-2",
            "user-1",
            "conv-2",
            "s2",
            "INTERVIEW",
            Instant.parse("2024-01-02T00:00:00Z")
        ));

        jobPostRepository.save(new com.jobagent.server.store.JobPostEntity(
            "job-2",
            "task-1",
            "boss",
            "ext-2",
            "Title B",
            "Company B",
            "Shanghai",
            "20k-30k",
            "3y",
            "raw",
            "{}",
            "ACTIVE",
            Instant.parse("2024-01-02T00:00:00Z")
        ));
        conversationRepository.save(new com.jobagent.server.store.ConversationEntity(
            "conv-2",
            "task-1",
            "job-2",
            "ext-conv-2",
            "NEW",
            null,
            null,
            null,
            Instant.parse("2024-01-02T00:00:00Z")
        ));

        var snapshot = store.snapshot("user-1");

        assertThat(snapshot.metrics().recommendations()).isEqualTo(1);
        assertThat(snapshot.metrics().drafts()).isEqualTo(1);
        assertThat(snapshot.metrics().replies()).isEqualTo(1);
        assertThat(snapshot.metrics().interviews()).isEqualTo(1);
        assertThat(snapshot.recommendations().get(0).title()).isEqualTo("B");
        assertThat(snapshot.drafts().get(0).conversationId()).isEqualTo("conv-2");
        assertThat(snapshot.replies().get(0).summary()).isEqualTo("s2");
        assertThat(snapshot.interviews().get(0).company()).isEqualTo("Company B");
        assertThat(snapshot.interviews().get(0).title()).isEqualTo("Title B");
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @Bean
        DashboardStore dashboardStore(DashboardRecommendationRepository recommendationRepository,
                                      DashboardDraftRepository draftRepository,
                                      DashboardReplyRepository replyRepository,
                                      ConversationRepository conversationRepository,
                                      JobPostRepository jobPostRepository,
                                      ObjectMapper objectMapper,
                                      Environment environment) {
            int maxItems = Integer.parseInt(environment.getProperty("job-agent.dashboard.max-items", "20"));
            return new DashboardStore(
                recommendationRepository,
                draftRepository,
                replyRepository,
                conversationRepository,
                jobPostRepository,
                objectMapper,
                maxItems
            );
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
