package com.jobagent.server.dto;

public record PluginErrorResponse(
    String code,
    String message,
    Object payload
) {}
