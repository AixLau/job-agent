package com.jobagent.server.dto;

import java.util.List;

public record RecommendationItem(
    String title,
    String company,
    int score,
    List<String> reasons
) {}
