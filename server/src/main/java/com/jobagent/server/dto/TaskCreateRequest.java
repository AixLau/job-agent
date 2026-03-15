package com.jobagent.server.dto;

import java.util.List;

public record TaskCreateRequest(
    String title,
    String city,
    String salary,
    String experience,
    List<String> exclude,
    List<String> preferences,
    String automationLevel,
    String strategyText
) {}
