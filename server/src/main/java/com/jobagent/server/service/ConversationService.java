package com.jobagent.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.server.dto.ActionHint;
import com.jobagent.server.dto.ActionReportRequest;
import com.jobagent.server.dto.ChatReportRequest;
import com.jobagent.server.dto.ChatReportResponse;
import com.jobagent.server.dto.ConversationCloseResponse;
import com.jobagent.server.dto.ConversationDetailResponse;
import com.jobagent.server.dto.ConversationSummary;
import com.jobagent.server.dto.DraftApproveResponse;
import com.jobagent.server.dto.DraftContent;
import com.jobagent.server.dto.DraftRegenerateResponse;
import com.jobagent.server.dto.DraftRejectResponse;
import com.jobagent.server.dto.DraftItem;
import com.jobagent.server.dto.DraftSummary;
import com.jobagent.server.dto.ReplyItem;
import com.jobagent.server.dto.ReplyResult;
import com.jobagent.server.dto.WorkerFollowUpResponse;
import com.jobagent.server.dto.WorkerReplyClassifyRequest;
import com.jobagent.server.dto.WorkerReplyClassifyResponse;
import com.jobagent.server.repository.ConversationRepository;
import com.jobagent.server.repository.JobMatchRepository;
import com.jobagent.server.repository.JobPostRepository;
import com.jobagent.server.repository.MessageDraftRepository;
import com.jobagent.server.repository.MessageRepository;
import com.jobagent.server.repository.TaskRepository;
import com.jobagent.server.store.ConversationEntity;
import com.jobagent.server.store.JobPostEntity;
import com.jobagent.server.store.MessageDraftEntity;
import com.jobagent.server.store.MessageEntity;
import com.jobagent.server.store.DashboardStore;
import jakarta.validation.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
public class ConversationService {

    private static final String STATUS_NEW = "NEW";
    private static final String STATUS_NEEDS_REPLY = "NEEDS_REPLY";
    private static final String STATUS_INTERVIEW = "INTERVIEW";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String STATUS_REPLIED = "REPLIED";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final String STATUS_WAITING_HR = "WAITING_HR";
    private static final String SOURCE_TYPE_SYSTEM = "SYSTEM";

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final JobMatchRepository jobMatchRepository;
    private final JobPostRepository jobPostRepository;
    private final MessageDraftRepository messageDraftRepository;
    private final TaskRepository taskRepository;
    private final WorkerClient workerClient;
    private final FollowUpPolicyService followUpPolicyService;
    private final ModelOutputValidator validator;
    private final DuplicatePayloadBuilder duplicatePayloadBuilder;
    private final DashboardStore dashboardStore;
    private final ObjectMapper objectMapper;

    public ConversationService(ConversationRepository conversationRepository,
                               MessageRepository messageRepository,
                               JobMatchRepository jobMatchRepository,
                               JobPostRepository jobPostRepository,
                               MessageDraftRepository messageDraftRepository,
                               TaskRepository taskRepository,
                               WorkerClient workerClient,
                               FollowUpPolicyService followUpPolicyService,
                               ModelOutputValidator validator,
                               DuplicatePayloadBuilder duplicatePayloadBuilder,
                               DashboardStore dashboardStore,
                               ObjectMapper objectMapper) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.jobMatchRepository = jobMatchRepository;
        this.jobPostRepository = jobPostRepository;
        this.messageDraftRepository = messageDraftRepository;
        this.taskRepository = taskRepository;
        this.workerClient = workerClient;
        this.followUpPolicyService = followUpPolicyService;
        this.validator = validator;
        this.duplicatePayloadBuilder = duplicatePayloadBuilder;
        this.dashboardStore = dashboardStore;
        this.objectMapper = objectMapper;
    }

    public ChatReportResponse handleChatReport(ChatReportRequest request, String userId) {
        var task = requireTaskOwned(request.taskId(), userId);
        ConversationEntity conversation = conversationRepository
            .findByTaskIdAndExternalId(request.taskId(), request.conversationId())
            .orElseGet(() -> new ConversationEntity(
                UUID.randomUUID().toString(),
                request.taskId(),
                null,
                request.conversationId(),
                STATUS_NEW,
                null,
                null,
                null,
                null
            ));
        conversationRepository.save(conversation);

        if (request.lastMessageId() != null) {
            boolean exists = messageRepository.findByConversationIdAndExternalId(
                conversation.getId(),
                request.lastMessageId()
            ).isPresent();
            if (exists) {
                throw new DuplicateResponseException(duplicatePayloadBuilder.chatReport(conversation));
            }
        }

        persistMessages(conversation.getId(), request.messages());

        CompletableFuture<WorkerReplyClassifyResponse> replyFuture = CompletableFuture.supplyAsync(() -> callReplyClassify(request, conversation));
        CompletableFuture<WorkerFollowUpResponse> followUpFuture = CompletableFuture.supplyAsync(() -> callFollowUp(request, conversation));

        WorkerReplyClassifyResponse response = join(replyFuture);
        WorkerFollowUpResponse followUp = join(followUpFuture);
        if (followUp == null) {
            followUp = fallbackFollowUp(response);
        }
        validator.validateReplyClassify(response);
        validator.validateFollowUp(followUp);
        try {
            validator.validateSummary(response.summary());
        } catch (ValidationException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
        }

        conversation.setLastIntent(response.intent());
        conversation.setLastSummary(response.summary());
        conversation.setLastAction(firstNonBlank(followUp.nextAction(), response.nextAction()));
        conversation.setPriority(followUp.priority());
        conversation.setFollowUpAt(resolveFollowUpAt(followUp.followUpHours()));

        applyStatus(conversation, response.intent(), followUp.suggestedStatus());
        conversationRepository.save(conversation);

        ReplyResult reply = new ReplyResult(response.intent(), response.summary(), response.nextAction());
        dashboardStore.addReply(userId, new ReplyItem(
            conversation.getId(),
            conversation.getJobPostId(),
            resolveCompany(conversation.getJobPostId()),
            reply.summary(),
            reply.intent(),
            conversation.getLastAction(),
            conversation.getPriority(),
            conversation.getFollowUpAt(),
            Instant.now()
        ));

        MessageDraftEntity draftEntity = upsertSystemDraft(conversation.getId(), firstNonBlank(
            followUp.draftContent(),
            existingDraftContent(conversation.getId())
        ));
        DraftContent draftContent = draftEntity == null ? null : new DraftContent(defaultString(draftEntity.getContent(), ""));
        if (draftEntity != null) {
            dashboardStore.addDraft(userId, new DraftItem(
                draftEntity.getId(),
                draftEntity.getConversationId(),
                conversation.getJobPostId(),
                resolveCompany(conversation.getJobPostId()),
                draftEntity.getContent(),
                draftEntity.getCreatedAt(),
                draftEntity.isApproved()
            ));
        }
        boolean autoSend = draftContent != null
            && !Boolean.TRUE.equals(followUp.requiresReview())
            && isAutoSendAllowed(task, conversation);
        ActionHint actionHint = draftContent == null ? null : new ActionHint(draftContent.content());
        return new ChatReportResponse("ok", reply, autoSend, draftContent, actionHint);
    }

    public ConversationDetailResponse detail(String conversationId, String userId) {
        ConversationEntity conversation = requireConversationOwned(conversationId, userId);
        return new ConversationDetailResponse(toSummary(conversation));
    }

    public DraftApproveResponse approveDraft(String draftId, String userId) {
        MessageDraftEntity draft = messageDraftRepository.findById(draftId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "draft not found"));
        requireConversationOwned(draft.getConversationId(), userId);
        draft.setApproved(true);
        messageDraftRepository.save(draft);
        dashboardStore.updateDraftApproval(draftId, true);
        DraftSummary summary = new DraftSummary(draft.isApproved());
        ActionHint actionHint = new ActionHint(defaultString(draft.getContent(), ""));
        return new DraftApproveResponse("ok", summary, actionHint);
    }

    public DraftRejectResponse rejectDraft(String draftId, String userId) {
        MessageDraftEntity draft = messageDraftRepository.findById(draftId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "draft not found"));
        requireConversationOwned(draft.getConversationId(), userId);
        draft.setApproved(false);
        messageDraftRepository.save(draft);
        dashboardStore.updateDraftApproval(draftId, false);
        DraftSummary summary = new DraftSummary(draft.isApproved());
        return new DraftRejectResponse("ok", summary);
    }

    public ConversationCloseResponse close(String conversationId, String userId) {
        ConversationEntity conversation = requireConversationOwned(conversationId, userId);
        conversation.setStatus(STATUS_CLOSED);
        conversationRepository.save(conversation);
        return new ConversationCloseResponse("ok", toSummary(conversation));
    }

    public DraftRegenerateResponse regenerate(String conversationId, String userId) {
        requireConversationOwned(conversationId, userId);
        MessageDraftEntity draft = messageDraftRepository.findByConversationIdAndSourceType(
            conversationId,
            SOURCE_TYPE_SYSTEM
        ).orElse(null);
        String content = draft == null ? "draft pending" : defaultString(draft.getContent(), "");
        return new DraftRegenerateResponse("ok", new DraftContent(content));
    }

    public void handleActionReport(ActionReportRequest request, String userId) {
        requireTaskOwned(request.taskId(), userId);
        if (request.payload() == null) {
            return;
        }
        String conversationExternalId = valueOf(request.payload(), "conversation_id");
        if (conversationExternalId == null || conversationExternalId.isBlank()) {
            return;
        }
        ConversationEntity conversation = conversationRepository
            .findByTaskIdAndExternalId(request.taskId(), conversationExternalId)
            .orElse(null);
        if (conversation == null) {
            return;
        }
        String actionType = request.actionType();
        if (actionType == null || actionType.isBlank()) {
            return;
        }
        String normalized = actionType.trim().toUpperCase(java.util.Locale.ROOT);
        String conversationStatus;
        String jobPostStatus;
        if ("DELIVERED".equals(normalized)) {
            conversationStatus = "WAITING_HR";
            jobPostStatus = "WAITING_HR";
        } else if ("SEND".equals(normalized) || "SEND_MESSAGE".equals(normalized)) {
            conversationStatus = "SENT";
            jobPostStatus = "SENT";
        } else {
            return;
        }

        conversation.setStatus(conversationStatus);
        conversationRepository.save(conversation);
        if (conversation.getJobPostId() != null) {
            JobPostEntity post = jobPostRepository.findById(conversation.getJobPostId()).orElse(null);
            if (post != null) {
                post.setStatus(jobPostStatus);
                jobPostRepository.save(post);
            }
        }
    }

    private void persistMessages(String conversationId, List<Map<String, Object>> messages) {
        if (messages == null) {
            return;
        }
        for (Map<String, Object> message : messages) {
            if (message == null) {
                continue;
            }
            String externalId = valueOf(message, "id");
            if (externalId == null || externalId.isBlank()) {
                externalId = UUID.randomUUID().toString();
            }
            boolean exists = messageRepository.findByConversationIdAndExternalId(conversationId, externalId).isPresent();
            if (exists) {
                continue;
            }
            String role = valueOf(message, "role");
            String content = firstNonBlank(
                valueOf(message, "text"),
                valueOf(message, "content"),
                valueOf(message, "message")
            );
            MessageEntity entity = new MessageEntity(
                UUID.randomUUID().toString(),
                conversationId,
                role,
                content,
                externalId,
                Instant.now()
            );
            messageRepository.save(entity);
        }
    }

    private WorkerReplyClassifyResponse callReplyClassify(ChatReportRequest request, ConversationEntity conversation) {
        WorkerReplyClassifyRequest workerRequest = new WorkerReplyClassifyRequest(
            request.taskId(),
            "REPLY_CLASSIFY",
            Map.of("id", conversation.getId(), "external_id", conversation.getExternalId()),
            request.messages(),
            request.lastMessageId(),
            IdempotencyKeys.replyClassify(request.taskId(), request.conversationId(), request.lastMessageId())
        );
        try {
            return workerClient.replyClassify(workerRequest);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "WORKER_TIMEOUT");
        }
    }

    private WorkerFollowUpResponse callFollowUp(ChatReportRequest request, ConversationEntity conversation) {
        return followUpPolicyService.plan(
            request.taskId(),
            Map.of("id", conversation.getId(), "external_id", conversation.getExternalId()),
            request.messages(),
            request.lastMessageId(),
            IdempotencyKeys.followUp(request.taskId(), request.conversationId(), request.lastMessageId())
        );
    }

    private void applyStatus(ConversationEntity conversation, String intent, String suggestedStatus) {
        String status;
        if ("INTERVIEW".equalsIgnoreCase(suggestedStatus) || "INTERVIEW".equalsIgnoreCase(intent)) {
            status = STATUS_INTERVIEW;
        } else if ("WAITING_HR".equalsIgnoreCase(suggestedStatus)) {
            status = STATUS_WAITING_HR;
        } else if ("NEEDS_REPLY".equalsIgnoreCase(suggestedStatus)) {
            status = STATUS_NEEDS_REPLY;
        } else if ("REJECTED".equalsIgnoreCase(intent)) {
            status = STATUS_CLOSED;
        } else {
            status = STATUS_NEEDS_REPLY;
        }
        conversation.setStatus(status);

        if (conversation.getJobPostId() != null) {
            JobPostEntity post = jobPostRepository.findById(conversation.getJobPostId()).orElse(null);
            if (post != null) {
                if (STATUS_INTERVIEW.equals(status)) {
                    post.setStatus(STATUS_INTERVIEW);
                } else if (STATUS_WAITING_HR.equals(status)) {
                    post.setStatus(STATUS_WAITING_HR);
                } else if (STATUS_CLOSED.equals(status)) {
                    post.setStatus(STATUS_ARCHIVED);
                } else {
                    post.setStatus(STATUS_REPLIED);
                }
                jobPostRepository.save(post);
            }
        }
    }

    private ConversationEntity requireConversationOwned(String conversationId, String userId) {
        ConversationEntity conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "conversation not found"));
        requireTaskOwned(conversation.getTaskId(), userId);
        return conversation;
    }

    private com.jobagent.server.store.TaskEntity requireTaskOwned(String taskId, String userId) {
        return taskRepository.findByIdAndUserId(taskId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "task not found"));
    }

    private boolean isAutoSendAllowed(com.jobagent.server.store.TaskEntity task, ConversationEntity conversation) {
        if (task == null || !"AUTO".equalsIgnoreCase(defaultString(task.getAutomationLevel(), ""))) {
            return false;
        }
        if (conversation.getJobPostId() == null || conversation.getJobPostId().isBlank()) {
            return false;
        }
        return readRiskTags(task.getId(), conversation.getJobPostId()).isEmpty();
    }

    private List<String> readRiskTags(String taskId, String jobPostId) {
        var match = jobMatchRepository.findByTaskIdAndJobPostId(taskId, jobPostId).orElse(null);
        if (match == null || match.getRiskTagsJson() == null || match.getRiskTagsJson().isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(match.getRiskTagsJson(), new TypeReference<>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private MessageDraftEntity upsertSystemDraft(String conversationId, String content) {
        MessageDraftEntity existing = messageDraftRepository.findByConversationIdAndSourceType(conversationId, SOURCE_TYPE_SYSTEM)
            .orElse(null);
        if (content == null || content.isBlank()) {
            return existing;
        }
        if (existing != null && content.equals(existing.getContent())) {
            return existing;
        }
        try {
            validator.validateDraft(content);
        } catch (ValidationException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
        }
        MessageDraftEntity draft = existing == null
            ? new MessageDraftEntity(
                UUID.randomUUID().toString(),
                conversationId,
                content,
                SOURCE_TYPE_SYSTEM,
                false,
                null
            )
            : existing;
        draft.setContent(content);
        messageDraftRepository.save(draft);
        return draft;
    }

    private String existingDraftContent(String conversationId) {
        return messageDraftRepository.findByConversationIdAndSourceType(conversationId, SOURCE_TYPE_SYSTEM)
            .map(MessageDraftEntity::getContent)
            .orElse(null);
    }

    private WorkerFollowUpResponse fallbackFollowUp(WorkerReplyClassifyResponse response) {
        String intent = response.intent() == null ? "" : response.intent();
        String suggestedStatus = "INTERVIEW".equalsIgnoreCase(intent) ? STATUS_INTERVIEW : STATUS_NEEDS_REPLY;
        Integer followUpHours = "INTERVIEW".equalsIgnoreCase(intent) ? 0 : 4;
        return new WorkerFollowUpResponse(
            "NORMAL",
            suggestedStatus,
            response.nextAction(),
            followUpHours,
            null,
            false
        );
    }

    private String resolveCompany(String jobPostId) {
        if (jobPostId == null || jobPostId.isBlank()) {
            return null;
        }
        return jobPostRepository.findById(jobPostId)
            .map(JobPostEntity::getCompany)
            .orElse(null);
    }

    private Instant resolveFollowUpAt(Integer followUpHours) {
        int hours = followUpHours == null ? 0 : Math.max(followUpHours, 0);
        return Instant.now().plusSeconds(hours * 3600L);
    }

    private <T> T join(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof ResponseStatusException responseStatusException) {
                throw responseStatusException;
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw ex;
        }
    }

    private ConversationSummary toSummary(ConversationEntity conversation) {
        return new ConversationSummary(
            conversation.getId(),
            conversation.getStatus(),
            conversation.getLastIntent(),
            conversation.getLastSummary()
        );
    }

    private String defaultString(String value, String fallback) {
        return value == null ? fallback : value;
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
}
