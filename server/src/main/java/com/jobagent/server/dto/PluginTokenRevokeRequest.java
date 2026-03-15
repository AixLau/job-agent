package com.jobagent.server.dto;

import jakarta.validation.constraints.NotBlank;

public record PluginTokenRevokeRequest(
    @NotBlank String pluginToken,
    @NotBlank String browserId
) {}
