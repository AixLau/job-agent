package com.jobagent.server.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.server.dto.DashboardMetrics;
import com.jobagent.server.dto.DashboardResponse;
import com.jobagent.server.dto.DraftItem;
import com.jobagent.server.dto.InterviewItem;
import com.jobagent.server.dto.RecommendationItem;
import com.jobagent.server.dto.ReplyItem;
import com.jobagent.server.repository.DashboardDraftRepository;
import com.jobagent.server.repository.DashboardRecommendationRepository;
import com.jobagent.server.repository.DashboardReplyRepository;
import com.jobagent.server.repository.ConversationRepository;
import com.jobagent.server.repository.JobPostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class DashboardStore {

    private static final Logger log = LoggerFactory.getLogger(DashboardStore.class);
    private static final List<String> EMPTY_REASONS = List.of();

    private final DashboardRecommendationRepository recommendationRepository;
    private final DashboardDraftRepository draftRepository;
    private final DashboardReplyRepository replyRepository;
    private final ConversationRepository conversationRepository;
    private final JobPostRepository jobPostRepository;
    private final ObjectMapper objectMapper;
    private final int maxItems;

    public DashboardStore(DashboardRecommendationRepository recommendationRepository,
                          DashboardDraftRepository draftRepository,
                          DashboardReplyRepository replyRepository,
                          ConversationRepository conversationRepository,
                          JobPostRepository jobPostRepository,
                          ObjectMapper objectMapper,
                          @Value("${job-agent.dashboard.max-items:20}") int maxItems) {
        this.recommendationRepository = recommendationRepository;
        this.draftRepository = draftRepository;
        this.replyRepository = replyRepository;
        this.conversationRepository = conversationRepository;
        this.jobPostRepository = jobPostRepository;
        this.objectMapper = objectMapper;
        this.maxItems = maxItems;
    }

    public void addRecommendation(String userId, RecommendationItem item) {
        String risksJson = writeRisks(item.risks());
        DashboardRecommendationEntity entity = new DashboardRecommendationEntity(
            UUID.randomUUID().toString(),
            userId,
            item.jobPostId(),
            item.title(),
            item.company(),
            item.score(),
            risksJson,
            item.status()
        );
        try {
            recommendationRepository.save(entity);
        } catch (RuntimeException ex) {
            log.warn("Failed to persist recommendation", ex);
        }
    }

    public void addDraft(String userId, DraftItem item) {
        String draftId = item.draftId() == null || item.draftId().isBlank()
            ? UUID.randomUUID().toString()
            : item.draftId();
        DashboardDraftEntity entity = new DashboardDraftEntity(
            draftId,
            userId,
            item.conversationId(),
            item.content(),
            item.approved(),
            item.createdAt()
        );
        try {
            draftRepository.save(entity);
        } catch (RuntimeException ex) {
            log.warn("Failed to persist draft", ex);
        }
    }

    public void updateDraftApproval(String draftId, boolean approved) {
        if (draftId == null || draftId.isBlank()) {
            return;
        }
        DashboardDraftEntity entity = draftRepository.findById(draftId).orElse(null);
        if (entity == null) {
            return;
        }
        entity.setApproved(approved);
        try {
            draftRepository.save(entity);
        } catch (RuntimeException ex) {
            log.warn("Failed to update draft approval", ex);
        }
    }

    public void addReply(String userId, ReplyItem item) {
        DashboardReplyEntity entity = new DashboardReplyEntity(
            UUID.randomUUID().toString(),
            userId,
            item.conversationId(),
            item.summary(),
            item.intent(),
            item.updatedAt()
        );
        try {
            replyRepository.save(entity);
        } catch (RuntimeException ex) {
            log.warn("Failed to persist reply", ex);
        }
    }

    public DashboardResponse snapshot(String userId) {
        try {
            PageRequest page = PageRequest.of(0, maxItems);
            List<RecommendationItem> recList = recommendationRepository
                .findAllByUserIdOrderByCreatedAtDescIdDesc(userId, page)
                .stream()
                .map(this::toRecommendation)
                .toList();
            List<DraftItem> draftList = draftRepository
                .findAllByUserIdOrderByCreatedAtDescIdDesc(userId, page)
                .stream()
                .map(this::toDraft)
                .toList();
            List<ReplyItem> replyList = replyRepository
                .findAllByUserIdOrderByUpdatedAtDescIdDesc(userId, page)
                .stream()
                .map(this::toReply)
                .toList();

            List<InterviewItem> interviews = replyList.stream()
                .filter(item -> "INTERVIEW".equalsIgnoreCase(item.intent()))
                .map(item -> {
                    String company = "";
                    String title = "";
                    var conversation = conversationRepository.findById(item.conversationId()).orElse(null);
                    if (conversation != null && conversation.getJobPostId() != null) {
                        var post = jobPostRepository.findById(conversation.getJobPostId()).orElse(null);
                        if (post != null) {
                            company = post.getCompany() == null ? "" : post.getCompany();
                            title = post.getTitle() == null ? "" : post.getTitle();
                        }
                    }
                    return new InterviewItem(item.conversationId(), company, title, item.updatedAt());
                })
                .toList();

            DashboardMetrics metrics = new DashboardMetrics(
                recList.size(),
                draftList.size(),
                replyList.size(),
                interviews.size()
            );

            return new DashboardResponse(metrics, recList, draftList, replyList, interviews, java.time.Instant.now());
        } catch (RuntimeException ex) {
            log.warn("Failed to load dashboard snapshot", ex);
            return emptyResponse();
        }
    }

    public void clear() {
        replyRepository.deleteAll();
        draftRepository.deleteAll();
        recommendationRepository.deleteAll();
    }

    private RecommendationItem toRecommendation(DashboardRecommendationEntity entity) {
        return new RecommendationItem(
            entity.getJobPostId(),
            entity.getTitle(),
            entity.getCompany(),
            entity.getScore(),
            readRisks(entity.getRisksJson()),
            entity.getStatus()
        );
    }

    private DraftItem toDraft(DashboardDraftEntity entity) {
        return new DraftItem(
            entity.getId(),
            entity.getConversationId(),
            entity.getContent(),
            entity.getCreatedAt(),
            entity.isApproved()
        );
    }

    private ReplyItem toReply(DashboardReplyEntity entity) {
        return new ReplyItem(
            entity.getConversationId(),
            entity.getSummary(),
            entity.getIntent(),
            entity.getUpdatedAt()
        );
    }

    private String writeRisks(List<String> risks) {
        List<String> safeRisks = risks == null ? EMPTY_REASONS : risks;
        try {
            return objectMapper.writeValueAsString(safeRisks);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    private List<String> readRisks(String risksJson) {
        if (risksJson == null || risksJson.isBlank()) {
            return EMPTY_REASONS;
        }
        try {
            return objectMapper.readValue(risksJson, new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            return EMPTY_REASONS;
        }
    }

    private DashboardResponse emptyResponse() {
        DashboardMetrics metrics = new DashboardMetrics(0, 0, 0, 0);
        return new DashboardResponse(metrics, List.of(), List.of(), List.of(), List.of(), java.time.Instant.now());
    }
}
