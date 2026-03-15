package com.jobagent.server.dto;

import java.util.List;

public record TaskListResponse(
    List<TaskResponse> tasks
) {}
