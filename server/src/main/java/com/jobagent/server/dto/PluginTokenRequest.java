package com.jobagent.server.dto;

import jakarta.validation.constraints.NotBlank;

public record PluginTokenRequest(
    @NotBlank String accessToken,
    @NotBlank String browserId
) {}
