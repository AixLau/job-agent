package com.jobagent.server.dto;

import java.time.Instant;

public record FollowItem(
    String jobPostId,
    String title,
    String company,
    Instant createdAt
) {}
