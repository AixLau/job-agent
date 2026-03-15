package com.jobagent.server.dto;

import java.util.List;

public record TaskUpdateRequest(
    String title,
    String city,
    String salary,
    String experience,
    List<String> exclude,
    List<String> preferences,
    String status,
    String automationLevel,
    String strategyText
) {}
