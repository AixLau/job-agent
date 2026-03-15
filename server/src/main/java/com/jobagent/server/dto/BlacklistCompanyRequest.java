package com.jobagent.server.dto;

public record BlacklistCompanyRequest(
    String companyName,
    String source
) {}
