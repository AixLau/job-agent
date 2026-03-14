package com.jobagent.server.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.server.dto.DashboardMetrics;
import com.jobagent.server.dto.DashboardResponse;
import com.jobagent.server.dto.DraftItem;
import com.jobagent.server.dto.RecommendationItem;
import com.jobagent.server.dto.ReplyItem;
import com.jobagent.server.repository.DashboardDraftRepository;
import com.jobagent.server.repository.DashboardRecommendationRepository;
import com.jobagent.server.repository.DashboardReplyRepository;
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
    private final ObjectMapper objectMapper;
    private final int maxItems;

    public DashboardStore(DashboardRecommendationRepository recommendationRepository,
                          DashboardDraftRepository draftRepository,
                          DashboardReplyRepository replyRepository,
                          ObjectMapper objectMapper,
                          @Value("${job-agent.dashboard.max-items:20}") int maxItems) {
        this.recommendationRepository = recommendationRepository;
        this.draftRepository = draftRepository;
        this.replyRepository = replyRepository;
        this.objectMapper = objectMapper;
        this.maxItems = maxItems;
    }

    public void addRecommendation(RecommendationItem item) {
        String reasonsJson = writeReasons(item.reasons());
        DashboardRecommendationEntity entity = new DashboardRecommendationEntity(
            UUID.randomUUID().toString(),
            item.title(),
            item.company(),
            item.score(),
            reasonsJson
        );
        try {
            recommendationRepository.save(entity);
        } catch (RuntimeException ex) {
            log.warn("Failed to persist recommendation", ex);
        }
    }

    public void addDraft(DraftItem item) {
        DashboardDraftEntity entity = new DashboardDraftEntity(
            UUID.randomUUID().toString(),
            item.company(),
            item.title(),
            item.content()
        );
        try {
            draftRepository.save(entity);
        } catch (RuntimeException ex) {
            log.warn("Failed to persist draft", ex);
        }
    }

    public void addReply(ReplyItem item) {
        DashboardReplyEntity entity = new DashboardReplyEntity(
            UUID.randomUUID().toString(),
            item.company(),
            item.intent(),
            item.summary(),
            item.nextAction()
        );
        try {
            replyRepository.save(entity);
        } catch (RuntimeException ex) {
            log.warn("Failed to persist reply", ex);
        }
    }

    public DashboardResponse snapshot() {
        try {
            PageRequest page = PageRequest.of(0, maxItems);
            List<RecommendationItem> recList = recommendationRepository
                .findAllByOrderByCreatedAtDescIdDesc(page)
                .stream()
                .map(this::toRecommendation)
                .toList();
            List<DraftItem> draftList = draftRepository
                .findAllByOrderByCreatedAtDescIdDesc(page)
                .stream()
                .map(this::toDraft)
                .toList();
            List<ReplyItem> replyList = replyRepository
                .findAllByOrderByCreatedAtDescIdDesc(page)
                .stream()
                .map(this::toReply)
                .toList();

            int interviews = (int) replyList.stream()
                .filter(item -> "INTERVIEW".equalsIgnoreCase(item.intent()))
                .count();

            DashboardMetrics metrics = new DashboardMetrics(
                recList.size(),
                draftList.size(),
                replyList.size(),
                interviews
            );

            return new DashboardResponse(metrics, recList, draftList, replyList);
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
            entity.getTitle(),
            entity.getCompany(),
            entity.getScore(),
            readReasons(entity.getReasonsJson())
        );
    }

    private DraftItem toDraft(DashboardDraftEntity entity) {
        return new DraftItem(
            entity.getCompany(),
            entity.getTitle(),
            entity.getContent()
        );
    }

    private ReplyItem toReply(DashboardReplyEntity entity) {
        return new ReplyItem(
            entity.getCompany(),
            entity.getIntent(),
            entity.getSummary(),
            entity.getNextAction()
        );
    }

    private String writeReasons(List<String> reasons) {
        List<String> safeReasons = reasons == null ? EMPTY_REASONS : reasons;
        try {
            return objectMapper.writeValueAsString(safeReasons);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    private List<String> readReasons(String reasonsJson) {
        if (reasonsJson == null || reasonsJson.isBlank()) {
            return EMPTY_REASONS;
        }
        try {
            return objectMapper.readValue(reasonsJson, new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            return EMPTY_REASONS;
        }
    }

    private DashboardResponse emptyResponse() {
        DashboardMetrics metrics = new DashboardMetrics(0, 0, 0, 0);
        return new DashboardResponse(metrics, List.of(), List.of(), List.of());
    }
}
