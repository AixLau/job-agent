package com.jobagent.server.dto;

public record PluginTokenResponse(
    String pluginToken,
    long expiresIn
) {}
