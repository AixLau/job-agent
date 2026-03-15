package com.jobagent.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthRegisterRequest(
    @NotBlank String account,
    @NotBlank String password,
    @Email String email
) {}
