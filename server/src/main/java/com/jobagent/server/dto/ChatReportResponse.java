package com.jobagent.server.dto;

public record ChatReportResponse(
    String status,
    ReplyResult reply,
    boolean autoSend,
    DraftContent draft,
    ActionHint actionHint
) {}
