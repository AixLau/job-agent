package com.jobagent.server.dto;

public record DashboardMetrics(
    int recommendations,
    int drafts,
    int replies,
    int interviews
) {}
