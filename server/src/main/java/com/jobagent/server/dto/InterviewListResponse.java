package com.jobagent.server.dto;

import java.util.List;

public record InterviewListResponse(
    List<InterviewItem> items
) {}
