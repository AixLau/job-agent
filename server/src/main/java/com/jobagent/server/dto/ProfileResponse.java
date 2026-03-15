package com.jobagent.server.dto;

import java.time.Instant;
import java.util.List;

public record ProfileResponse(
    ProfilePayload profile
) {
    public record ProfilePayload(
        String account,
        String email,
        String fullName,
        String phone,
        String city,
        Integer yearsExperience,
        String summary,
        List<String> skills,
        String profileStatus,
        Instant updatedAt
    ) {}
}
