package com.jobagent.server.dto;

import java.util.List;

public record ProfileRequest(
    String fullName,
    String phone,
    String city,
    Integer yearsExperience,
    String summary,
    List<String> skills
) {}
