package com.jobagent.server.dto;

import java.util.List;

public record AnalysisResult(
    int score,
    List<String> reasons,
    List<String> riskTags
) {}
