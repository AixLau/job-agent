package com.jobagent.server.service;

import com.jobagent.server.dto.WorkerJobMatchResponse;
import com.jobagent.server.dto.WorkerFollowUpResponse;
import com.jobagent.server.dto.WorkerReplyClassifyResponse;
import jakarta.validation.ValidationException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class ModelOutputValidator {

    private static final int DRAFT_MIN = 10;
    private static final int DRAFT_MAX = 500;
    private static final int SUMMARY_MAX = 200;
    private static final int NEXT_ACTION_MAX = 100;
    private static final List<String> ALLOWED_INTENTS = List.of("INTERVIEW", "FOLLOW_UP", "REJECTED");
    private static final List<String> ALLOWED_PRIORITIES = List.of("HIGH", "NORMAL", "LOW");
    private static final List<String> ALLOWED_STATUSES = List.of("INTERVIEW", "WAITING_HR", "NEEDS_REPLY", "CLOSED");

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{11}\\b");

    private static final List<String> CONTACT_TOKENS = List.of(
        "\u5fae\u4fe1", // 微信
        "qq",
        "@"
    );

    private static final List<String> GUARANTEE_TOKENS = List.of(
        "\u4fdd\u8bc1", // 保证
        "\u786e\u4fdd", // 确保
        "100%",
        "\u767e\u5206\u767e", // 百分百
        "\u5305\u5f55", // 包录
        "\u5305\u8fc7"  // 包过
    );

    private static final List<String> SENSITIVE_TOKENS = List.of(
        "\u6d89\u653f", // 涉政
        "\u6d89\u9ec4", // 涉黄
        "\u8fb1\u9a82", // 辱骂
        "\u8d4c\u535a"  // 赌博
    );

    public void validateDraft(String content) {
        String value = content == null ? "" : content;
        int length = value.length();
        if (length < DRAFT_MIN || length > DRAFT_MAX) {
            throw new ValidationException("draft length out of range");
        }
        validateCommon(value);
    }

    public void validateSummary(String summary) {
        String value = summary == null ? "" : summary;
        if (value.length() > SUMMARY_MAX) {
            throw new ValidationException("summary too long");
        }
        validateCommon(value);
    }

    public void validateReplyClassify(WorkerReplyClassifyResponse response) {
        if (response == null) {
            throw new ValidationException("reply payload missing");
        }
        String intent = response.intent() == null ? "" : response.intent().trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_INTENTS.contains(intent)) {
            throw new ValidationException("invalid intent");
        }
        validateSummary(response.summary());
        String nextAction = response.nextAction() == null ? "" : response.nextAction().trim();
        if (nextAction.isBlank() || nextAction.length() > NEXT_ACTION_MAX) {
            throw new ValidationException("invalid next action");
        }
        validateCommon(nextAction);
    }

    public void validateJobMatch(WorkerJobMatchResponse response) {
        if (response == null) {
            throw new ValidationException("job match missing");
        }
        Integer score = response.score();
        if (score == null || score < 0 || score > 100) {
            throw new ValidationException("invalid score");
        }
        if (response.reasons() == null || response.reasons().isEmpty()) {
            throw new ValidationException("reasons required");
        }
        for (String reason : response.reasons()) {
            String value = reason == null ? "" : reason.trim();
            if (value.isBlank() || value.length() > SUMMARY_MAX) {
                throw new ValidationException("invalid reason");
            }
            validateCommon(value);
        }
        validateStringList(response.risks());
        validateParsedJob(response.parsedJob());
    }

    public void validateFollowUp(WorkerFollowUpResponse response) {
        if (response == null) {
            throw new ValidationException("follow up payload missing");
        }
        String priority = response.priority() == null ? "" : response.priority().trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_PRIORITIES.contains(priority)) {
            throw new ValidationException("invalid priority");
        }
        String suggestedStatus = response.suggestedStatus() == null
            ? ""
            : response.suggestedStatus().trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(suggestedStatus)) {
            throw new ValidationException("invalid suggested status");
        }
        String nextAction = response.nextAction() == null ? "" : response.nextAction().trim();
        if (nextAction.isBlank() || nextAction.length() > NEXT_ACTION_MAX) {
            throw new ValidationException("invalid next action");
        }
        validateCommon(nextAction);
        String draftContent = response.draftContent();
        if (draftContent != null && !draftContent.isBlank()) {
            validateDraft(draftContent);
        }
        Integer followUpHours = response.followUpHours();
        if (followUpHours == null || followUpHours < 0 || followUpHours > 168) {
            throw new ValidationException("invalid follow up hours");
        }
    }

    private void validateCommon(String value) {
        if (containsContact(value)) {
            throw new ValidationException("contact info not allowed");
        }
        if (containsToken(value, GUARANTEE_TOKENS)) {
            throw new ValidationException("guarantee statement not allowed");
        }
        if (containsToken(value, SENSITIVE_TOKENS)) {
            throw new ValidationException("sensitive content not allowed");
        }
    }

    private boolean containsContact(String value) {
        if (EMAIL_PATTERN.matcher(value).find()) {
            return true;
        }
        if (PHONE_PATTERN.matcher(value).find()) {
            return true;
        }
        return containsToken(value, CONTACT_TOKENS);
    }

    private boolean containsToken(String value, List<String> tokens) {
        String normalized = value.toLowerCase(Locale.ROOT);
        for (String token : tokens) {
            if (normalized.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private void validateStringList(List<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            String normalized = value == null ? "" : value.trim();
            if (normalized.isBlank() || normalized.length() > 40) {
                throw new ValidationException("invalid list item");
            }
        }
    }

    private void validateParsedJob(Map<String, Object> parsedJob) {
        if (parsedJob == null) {
            return;
        }
        validateRange(parsedJob, "salary_min", "salary_max");
        validateRange(parsedJob, "exp_min", "exp_max");
    }

    private void validateRange(Map<String, Object> parsedJob, String minKey, String maxKey) {
        Integer min = readInt(parsedJob.get(minKey));
        Integer max = readInt(parsedJob.get(maxKey));
        if (min != null && min < 0) {
            throw new ValidationException("invalid range");
        }
        if (max != null && max < 0) {
            throw new ValidationException("invalid range");
        }
        if (min != null && max != null && min > max) {
            throw new ValidationException("invalid range order");
        }
    }

    private Integer readInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new ValidationException("invalid numeric value");
        }
    }
}
