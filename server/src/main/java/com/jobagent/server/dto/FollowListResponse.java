package com.jobagent.server.dto;

import java.util.List;

public record FollowListResponse(
    List<FollowItem> items,
    int page,
    int size,
    long total
) {}
