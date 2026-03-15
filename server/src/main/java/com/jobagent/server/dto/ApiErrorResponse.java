package com.jobagent.server.dto;

public record ApiErrorResponse(
    ApiError error
) {
    public record ApiError(
        String code,
        String message
    ) {}
}
