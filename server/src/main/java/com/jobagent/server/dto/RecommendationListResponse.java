package com.jobagent.server.dto;

import java.util.List;

public record RecommendationListResponse(
    List<RecommendationItem> items
) {}
