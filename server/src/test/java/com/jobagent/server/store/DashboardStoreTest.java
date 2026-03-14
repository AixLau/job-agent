package com.jobagent.server.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.server.dto.DraftItem;
import com.jobagent.server.dto.RecommendationItem;
import com.jobagent.server.dto.ReplyItem;
import com.jobagent.server.repository.DashboardDraftRepository;
import com.jobagent.server.repository.DashboardRecommendationRepository;
import com.jobagent.server.repository.DashboardReplyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
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

    @Test
    void snapshotReturnsLatestAndMetrics() {
        store.addRecommendation(new RecommendationItem("A", "C1", 80, List.of("r1")));
        store.addDraft(new DraftItem("C1", "A", "d1"));
        store.addReply(new ReplyItem("C1", "INTERVIEW", "s1", "n1"));

        assertThat(recommendationRepository.count()).isEqualTo(1);
        assertThat(draftRepository.count()).isEqualTo(1);
        assertThat(replyRepository.count()).isEqualTo(1);

        recommendationRepository.deleteAll();
        recommendationRepository.save(new DashboardRecommendationEntity(
            "rec-1",
            "A",
            "C1",
            80,
            "[\"r1\"]",
            Instant.parse("2024-01-01T00:00:00Z")
        ));
        recommendationRepository.save(new DashboardRecommendationEntity(
            "rec-2",
            "B",
            "C1",
            81,
            "[\"r2\"]",
            Instant.parse("2024-01-02T00:00:00Z")
        ));

        var snapshot = store.snapshot();

        assertThat(snapshot.metrics().recommendations()).isEqualTo(1);
        assertThat(snapshot.metrics().drafts()).isEqualTo(1);
        assertThat(snapshot.metrics().replies()).isEqualTo(1);
        assertThat(snapshot.metrics().interviews()).isEqualTo(1);
        assertThat(snapshot.recommendations().get(0).title()).isEqualTo("B");
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @Bean
        DashboardStore dashboardStore(DashboardRecommendationRepository recommendationRepository,
                                      DashboardDraftRepository draftRepository,
                                      DashboardReplyRepository replyRepository,
                                      ObjectMapper objectMapper) {
            return new DashboardStore(recommendationRepository, draftRepository, replyRepository, objectMapper, 1);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
