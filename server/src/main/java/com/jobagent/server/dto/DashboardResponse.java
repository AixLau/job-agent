package com.jobagent.server.dto;

import java.util.List;

public record DashboardResponse(
    DashboardMetrics metrics,
    List<RecommendationItem> recommendations,
    List<DraftItem> drafts,
    List<ReplyItem> replies
) {}
