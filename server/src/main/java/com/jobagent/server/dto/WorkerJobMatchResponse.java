package com.jobagent.server.dto;

import java.util.List;

public record WorkerJobMatchResponse(
    Integer score,
    List<String> reasons,
    List<String> risks
) {}
