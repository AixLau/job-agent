package com.jobagent.server.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public final class IdempotencyKeys {

    private IdempotencyKeys() {
    }

    public static String goalParse(String taskId, String strategyText) {
        String normalized = normalize(strategyText);
        return taskId + ":GOAL_PARSE:" + sha256(normalized);
    }

    public static String jobMatch(String taskId, String externalId, String source) {
        return taskId + ":JOB_MATCH:" + safe(externalId) + ":" + safe(source);
    }

    public static String draft(String taskId, String conversationId, String externalId) {
        return taskId + ":DRAFT:" + safe(conversationId) + ":" + safe(externalId);
    }

    public static String replyClassify(String taskId, String conversationId, String lastMessageId) {
        return taskId + ":REPLY_CLASSIFY:" + safe(conversationId) + ":" + safe(lastMessageId);
    }

    public static String followUp(String taskId, String conversationId, String lastMessageId) {
        return taskId + ":FOLLOW_UP:" + safe(conversationId) + ":" + safe(lastMessageId);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Missing SHA-256", ex);
        }
    }
}
