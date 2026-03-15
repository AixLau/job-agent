package com.jobagent.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.server.dto.AnalysisResult;
import com.jobagent.server.dto.DraftItem;
import com.jobagent.server.dto.PageReportRequest;
import com.jobagent.server.dto.PageReportResponse;
import com.jobagent.server.dto.RecommendationItem;
import com.jobagent.server.dto.WorkerDraftRequest;
import com.jobagent.server.dto.WorkerDraftResponse;
import com.jobagent.server.dto.WorkerJobMatchRequest;
import com.jobagent.server.dto.WorkerJobMatchResponse;
import com.jobagent.server.repository.ConversationRepository;
import com.jobagent.server.repository.JobMatchRepository;
import com.jobagent.server.repository.JobPostRepository;
import com.jobagent.server.repository.MessageDraftRepository;
import com.jobagent.server.repository.ResumeRepository;
import com.jobagent.server.repository.TaskRepository;
import com.jobagent.server.repository.UserCompanyBlacklistRepository;
import com.jobagent.server.store.ConversationEntity;
import com.jobagent.server.store.JobMatchEntity;
import com.jobagent.server.store.JobPostEntity;
import com.jobagent.server.store.MessageDraftEntity;
import com.jobagent.server.store.ResumeEntity;
import com.jobagent.server.store.TaskEntity;
import com.jobagent.server.store.DashboardStore;
import jakarta.validation.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class JobPostService {

    private static final String STATUS_DISCOVERED = "DISCOVERED";
    private static final String STATUS_ANALYZED = "ANALYZED";
    private static final String STATUS_SHORTLISTED = "SHORTLISTED";
    private static final String STATUS_DRAFTED = "DRAFTED";
    private static final String STATUS_WAITING_USER = "WAITING_USER";
    private static final String SOURCE_TYPE_SYSTEM = "SYSTEM";

    private final JobPostRepository jobPostRepository;
    private final JobMatchRepository jobMatchRepository;
    private final MessageDraftRepository messageDraftRepository;
    private final ConversationRepository conversationRepository;
    private final TaskRepository taskRepository;
    private final ResumeRepository resumeRepository;
    private final WorkerClient workerClient;
    private final RuleEngineService ruleEngineService;
    private final ModelOutputValidator validator;
    private final DuplicatePayloadBuilder duplicatePayloadBuilder;
    private final DashboardStore dashboardStore;
    private final UserCompanyBlacklistRepository userCompanyBlacklistRepository;
    private final ObjectMapper mapper;

    public JobPostService(JobPostRepository jobPostRepository,
                          JobMatchRepository jobMatchRepository,
                          MessageDraftRepository messageDraftRepository,
                          ConversationRepository conversationRepository,
                          TaskRepository taskRepository,
                          ResumeRepository resumeRepository,
                          WorkerClient workerClient,
                          RuleEngineService ruleEngineService,
                          ModelOutputValidator validator,
                          DuplicatePayloadBuilder duplicatePayloadBuilder,
                          DashboardStore dashboardStore,
                          UserCompanyBlacklistRepository userCompanyBlacklistRepository,
                          ObjectMapper mapper) {
        this.jobPostRepository = jobPostRepository;
        this.jobMatchRepository = jobMatchRepository;
        this.messageDraftRepository = messageDraftRepository;
        this.conversationRepository = conversationRepository;
        this.taskRepository = taskRepository;
        this.resumeRepository = resumeRepository;
        this.workerClient = workerClient;
        this.ruleEngineService = ruleEngineService;
        this.validator = validator;
        this.duplicatePayloadBuilder = duplicatePayloadBuilder;
        this.dashboardStore = dashboardStore;
        this.userCompanyBlacklistRepository = userCompanyBlacklistRepository;
        this.mapper = mapper;
    }

    public PageReportResponse handlePageReport(PageReportRequest request, String userId) {
        Map<String, Object> extracted = safeMap(request.extractedJson());
        String source = firstNonBlank(valueOf(extracted, "source"), "unknown");
        String externalId = firstNonBlank(
            valueOf(extracted, "external_id"),
            request.sourceUrl(),
            request.domHash(),
            UUID.randomUUID().toString()
        );

        TaskEntity task = taskRepository.findByIdAndUserId(request.taskId(), userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "task not found"));
        ResumeEntity resume = resumeRepository.findFirstByUserIdOrderByCreatedAtDesc(userId).orElse(null);

        JobPostEntity existing = jobPostRepository.findBySourceAndExternalId(source, externalId).orElse(null);
        if (existing != null && request.taskId().equals(existing.getTaskId())) {
            JobMatchEntity match = jobMatchRepository.findByTaskIdAndJobPostId(request.taskId(), existing.getId())
                .orElse(null);
            MessageDraftEntity draft = findDraftForDuplicate(request, externalId);
            PageReportResponse payload = duplicatePayloadBuilder.pageReport(existing, match, draft);
            throw new DuplicateResponseException(payload);
        }

        String title = valueOf(extracted, "title");
        String company = valueOf(extracted, "company");

        JobPostEntity post;
        if (existing != null) {
            post = existing;
        } else {
            post = new JobPostEntity(
                UUID.randomUUID().toString(),
                request.taskId(),
                source,
                externalId,
                title,
                company,
                valueOf(extracted, "city"),
                valueOf(extracted, "salary"),
                valueOf(extracted, "experience"),
                request.rawText(),
                writeJson(extracted),
                STATUS_DISCOVERED,
                null
            );
            jobPostRepository.save(post);
        }

        WorkerJobMatchResponse matchResponse = callJobMatch(task, post, resume, extracted);
        int score = matchResponse.score() == null ? 0 : matchResponse.score();
        RuleResult.ParsedRange parsedRange = ruleEngineService.resolveParsedRange(
            matchResponse.parsedJob(),
            valueOf(extracted, "salary"),
            valueOf(extracted, "experience"),
            request.rawText()
        );
        RuleResult ruleResult = ruleEngineService.evaluateWithParsedRange(
            request.rawText(),
            task.getRuleConfigJson(),
            safeList(matchResponse.risks()),
            parsedRange
        );
        List<String> finalRiskTags = safeList(ruleResult.riskTags());
        boolean blacklisted = isBlacklisted(userId, source, defaultString(post.getCompany(), company));
        JobMatchEntity match = new JobMatchEntity(
            UUID.randomUUID().toString(),
            request.taskId(),
            post.getId(),
            score,
            writeJson(matchResponse.reasons()),
            writeJson(finalRiskTags),
            writeJson(ruleResult),
            null
        );
        jobMatchRepository.save(match);

        boolean visibleRecommendation = ruleResult.hardFilterPass() && !blacklisted;
        if (blacklisted) {
            post.setStatus("ARCHIVED");
        } else if (score >= 70 && visibleRecommendation) {
            post.setStatus(STATUS_SHORTLISTED);
        } else {
            post.setStatus(STATUS_ANALYZED);
        }
        jobPostRepository.save(post);

        AnalysisResult analysis = new AnalysisResult(score, safeList(matchResponse.reasons()), finalRiskTags);
        RecommendationItem recommendation = new RecommendationItem(
            post.getId(),
            defaultString(post.getTitle(), "未命名岗位"),
            defaultString(post.getCompany(), "未知公司"),
            analysis.score(),
            analysis.riskTags(),
            post.getStatus()
        );
        if (visibleRecommendation) {
            dashboardStore.addRecommendation(userId, recommendation);
        }

        DraftItem draftItem = null;
        if (visibleRecommendation && isDetailPage(request) && Boolean.TRUE.equals(request.wantDraft())) {
            draftItem = buildDraft(request, post, resume, extracted, userId);
        }

        return new PageReportResponse("ok", analysis, draftItem);
    }

    private WorkerJobMatchResponse callJobMatch(TaskEntity task, JobPostEntity post, ResumeEntity resume, Map<String, Object> extracted) {
        Map<String, Object> jobPost = buildJobPostMap(post, extracted);
        Map<String, Object> resumeMap = resume == null ? Collections.emptyMap() : readMap(resume.getParsedJson());
        Map<String, Object> strategy = readMap(task.getStrategyJson());
        WorkerJobMatchRequest workerRequest = new WorkerJobMatchRequest(
            task.getId(),
            "JOB_MATCH",
            jobPost,
            resumeMap,
            strategy,
            IdempotencyKeys.jobMatch(task.getId(), post.getExternalId(), post.getSource())
        );
        try {
            return workerClient.jobMatch(workerRequest);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "WORKER_TIMEOUT");
        }
    }

    private DraftItem buildDraft(PageReportRequest request,
                                 JobPostEntity post,
                                 ResumeEntity resume,
                                 Map<String, Object> extracted,
                                 String userId) {
        String externalId = post.getExternalId();
        String conversationExternalId = conversationExternalId(request, externalId);
        ConversationEntity conversation = conversationRepository.findByTaskIdAndExternalId(request.taskId(), conversationExternalId)
            .orElseGet(() -> new ConversationEntity(
                UUID.randomUUID().toString(),
                request.taskId(),
                post.getId(),
                conversationExternalId,
                STATUS_WAITING_USER,
                null,
                null,
                null,
                null
            ));
        conversation.setJobPostId(post.getId());
        conversation.setStatus(STATUS_WAITING_USER);
        conversationRepository.save(conversation);

        Map<String, Object> jobPost = buildJobPostMap(post, extracted);
        Map<String, Object> resumeMap = resume == null ? Collections.emptyMap() : readMap(resume.getParsedJson());
        WorkerDraftRequest workerRequest = new WorkerDraftRequest(
            request.taskId(),
            "DRAFT",
            Map.of("id", conversation.getId(), "external_id", conversation.getExternalId()),
            jobPost,
            resumeMap,
            IdempotencyKeys.draft(request.taskId(), conversationExternalId, externalId)
        );
        WorkerDraftResponse draftResponse;
        try {
            draftResponse = workerClient.buildDraft(workerRequest);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "WORKER_TIMEOUT");
        }
        try {
            validator.validateDraft(draftResponse.content());
        } catch (ValidationException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
        }

        MessageDraftEntity draft = new MessageDraftEntity(
            UUID.randomUUID().toString(),
            conversation.getId(),
            draftResponse.content(),
            SOURCE_TYPE_SYSTEM,
            false,
            null
        );
        messageDraftRepository.save(draft);

        post.setStatus(STATUS_DRAFTED);
        jobPostRepository.save(post);

        DraftItem draftItem = new DraftItem(
            draft.getId(),
            draft.getConversationId(),
            draftResponse.content(),
            draft.getCreatedAt(),
            draft.isApproved()
        );
        dashboardStore.addDraft(userId, draftItem);
        return draftItem;
    }

    private MessageDraftEntity findDraftForDuplicate(PageReportRequest request, String externalId) {
        String conversationExternalId = conversationExternalId(request, externalId);
        ConversationEntity conversation = conversationRepository.findByTaskIdAndExternalId(request.taskId(), conversationExternalId)
            .orElse(null);
        if (conversation == null) {
            return null;
        }
        return messageDraftRepository.findByConversationIdAndSourceType(conversation.getId(), SOURCE_TYPE_SYSTEM)
            .orElse(null);
    }

    private boolean isDetailPage(PageReportRequest request) {
        return request.pageType() != null && request.pageType().equalsIgnoreCase("detail");
    }

    private String conversationExternalId(PageReportRequest request, String externalId) {
        return firstNonBlank(request.sourceUrl(), request.domHash(), externalId);
    }

    private Map<String, Object> safeMap(Map<String, Object> value) {
        return value == null ? Collections.emptyMap() : value;
    }

    private List<String> safeList(List<String> value) {
        return value == null ? List.of() : value;
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String valueOf(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String writeJson(Object value) {
        try {
            return mapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            return Collections.emptyMap();
        }
    }

    private Map<String, Object> buildJobPostMap(JobPostEntity post, Map<String, Object> extracted) {
        Map<String, Object> jobPost = new java.util.LinkedHashMap<>();
        jobPost.put("id", post.getId());
        jobPost.put("title", post.getTitle());
        jobPost.put("company", post.getCompany());
        jobPost.put("city", post.getCity());
        jobPost.put("salary", post.getSalary());
        jobPost.put("experience", post.getExperience());
        jobPost.put("raw_text", post.getJdRaw());
        jobPost.put("extracted_json", extracted);
        return jobPost;
    }

    private boolean isBlacklisted(String userId, String source, String company) {
        if (userId == null || userId.isBlank() || source == null || source.isBlank() || company == null || company.isBlank()) {
            return false;
        }
        return userCompanyBlacklistRepository.existsByUserIdAndCompanyNameIgnoreCaseAndSource(
            userId,
            company.trim(),
            source.trim()
        );
    }
}
