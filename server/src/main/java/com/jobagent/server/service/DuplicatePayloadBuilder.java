package com.jobagent.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.server.dto.AnalysisResult;
import com.jobagent.server.dto.ChatReportResponse;
import com.jobagent.server.dto.DraftItem;
import com.jobagent.server.dto.PageReportResponse;
import com.jobagent.server.dto.ReplyResult;
import com.jobagent.server.store.ConversationEntity;
import com.jobagent.server.store.JobMatchEntity;
import com.jobagent.server.store.JobPostEntity;
import com.jobagent.server.store.MessageDraftEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DuplicatePayloadBuilder {

    private final ObjectMapper mapper;

    public DuplicatePayloadBuilder(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public PageReportResponse pageReport(JobPostEntity post,
                                         JobMatchEntity match,
                                         MessageDraftEntity draft) {
        AnalysisResult analysis = null;
        if (match != null) {
            int score = match.getScore() == null ? 0 : match.getScore();
            List<String> reasons = readList(match.getReasonJson());
            List<String> risks = readList(match.getRiskTagsJson());
            analysis = new AnalysisResult(score, reasons, risks);
        }
        DraftItem draftItem = null;
        if (draft != null) {
            draftItem = new DraftItem(
                draft.getId(),
                draft.getConversationId(),
                draft.getContent(),
                draft.getCreatedAt(),
                draft.isApproved()
            );
        }
        return new PageReportResponse("ok", analysis, draftItem);
    }

    public ChatReportResponse chatReport(ConversationEntity conversation) {
        ReplyResult reply = new ReplyResult(
            conversation.getLastIntent(),
            conversation.getLastSummary(),
            conversation.getLastAction()
        );
        return new ChatReportResponse("ok", reply, false, null, null);
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }
}
