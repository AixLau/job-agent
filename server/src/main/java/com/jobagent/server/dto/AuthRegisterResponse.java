package com.jobagent.server.dto;

public record AuthRegisterResponse(
    UserInfo user
) {
    public record UserInfo(
        String id,
        String account
    ) {}
}
