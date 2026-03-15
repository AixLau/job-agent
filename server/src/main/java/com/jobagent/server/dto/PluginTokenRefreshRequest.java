package com.jobagent.server.dto;

import jakarta.validation.constraints.NotBlank;

public record PluginTokenRefreshRequest(
    @NotBlank String pluginToken,
    @NotBlank String browserId
) {}
