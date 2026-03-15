package com.jobagent.server.dto;

import java.util.List;

public record RecommendationItem(
    String jobPostId,
    String title,
    String company,
    int score,
    List<String> risks,
    String status
) {}
