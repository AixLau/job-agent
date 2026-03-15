package com.jobagent.server.dto;

public record AuthLoginResponse(
    String accessToken,
    String refreshToken,
    long expiresIn
) {}
