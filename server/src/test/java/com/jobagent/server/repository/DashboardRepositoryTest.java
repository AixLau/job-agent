package com.jobagent.server.repository;

import com.jobagent.server.store.DashboardDraftEntity;
import com.jobagent.server.store.DashboardRecommendationEntity;
import com.jobagent.server.store.DashboardReplyEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DashboardRepositoryTest {

    @Autowired
    private DashboardRecommendationRepository recommendationRepository;

    @Autowired
    private DashboardDraftRepository draftRepository;

    @Autowired
    private DashboardReplyRepository replyRepository;

    @Test
    void saveAndLoadAllEntities() {
        DashboardRecommendationEntity rec = new DashboardRecommendationEntity(
            "rec-1",
            "资深产品经理",
            "智聘科技",
            88,
            "[\"岗位匹配\"]"
        );
        DashboardDraftEntity draft = new DashboardDraftEntity(
            "draft-1",
            "智聘科技",
            "资深产品经理",
            "您好，我对贵司岗位很感兴趣"
        );
        DashboardReplyEntity reply = new DashboardReplyEntity(
            "reply-1",
            "智聘科技",
            "INTERVIEW",
            "可以安排面试吗",
            "确认面试时间"
        );

        recommendationRepository.save(rec);
        draftRepository.save(draft);
        replyRepository.save(reply);

        DashboardRecommendationEntity recLoaded = recommendationRepository.findById("rec-1").orElseThrow();
        DashboardDraftEntity draftLoaded = draftRepository.findById("draft-1").orElseThrow();
        DashboardReplyEntity replyLoaded = replyRepository.findById("reply-1").orElseThrow();

        assertThat(recLoaded.getCompany()).isEqualTo("智聘科技");
        assertThat(draftLoaded.getContent()).contains("感兴趣");
        assertThat(replyLoaded.getIntent()).isEqualTo("INTERVIEW");
    }

    @Test
    void findLatestByCreatedAtThenId() {
        Instant timestamp = Instant.parse("2024-01-01T00:00:00Z");

        recommendationRepository.save(new DashboardRecommendationEntity(
            "rec-1",
            "产品经理",
            "智聘科技",
            80,
            "[\"r1\"]",
            timestamp
        ));
        recommendationRepository.save(new DashboardRecommendationEntity(
            "rec-2",
            "高级产品经理",
            "智聘科技",
            90,
            "[\"r2\"]",
            timestamp
        ));

        draftRepository.save(new DashboardDraftEntity(
            "draft-1",
            "智聘科技",
            "产品经理",
            "d1",
            timestamp
        ));
        draftRepository.save(new DashboardDraftEntity(
            "draft-2",
            "智聘科技",
            "高级产品经理",
            "d2",
            timestamp
        ));

        replyRepository.save(new DashboardReplyEntity(
            "reply-1",
            "智聘科技",
            "INTERVIEW",
            "s1",
            "n1",
            timestamp
        ));
        replyRepository.save(new DashboardReplyEntity(
            "reply-2",
            "智聘科技",
            "INTERVIEW",
            "s2",
            "n2",
            timestamp
        ));

        List<DashboardRecommendationEntity> recs = recommendationRepository
            .findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, 1));
        List<DashboardDraftEntity> drafts = draftRepository
            .findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, 1));
        List<DashboardReplyEntity> replies = replyRepository
            .findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, 1));

        assertThat(recs).hasSize(1);
        assertThat(drafts).hasSize(1);
        assertThat(replies).hasSize(1);

        assertThat(recs.get(0).getId()).isEqualTo("rec-2");
        assertThat(drafts.get(0).getId()).isEqualTo("draft-2");
        assertThat(replies.get(0).getId()).isEqualTo("reply-2");
    }
}
