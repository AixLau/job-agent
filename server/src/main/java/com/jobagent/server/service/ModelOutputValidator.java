package com.jobagent.server.service;

import jakarta.validation.ValidationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class ModelOutputValidator {

    private static final int DRAFT_MIN = 10;
    private static final int DRAFT_MAX = 500;
    private static final int SUMMARY_MAX = 200;

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
}
