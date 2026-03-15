package com.jobagent.server.dto;

import java.util.List;
import java.util.Map;

public record WorkerJobMatchResponse(
    Integer score,
    List<String> reasons,
    List<String> risks,
    Map<String, Object> parsedJob
) {}
