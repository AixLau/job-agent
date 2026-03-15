package com.jobagent.server.controller;

import com.jobagent.server.dto.AuthLoginRequest;
import com.jobagent.server.dto.AuthLoginResponse;
import com.jobagent.server.dto.AuthRegisterRequest;
import com.jobagent.server.dto.AuthRegisterResponse;
import com.jobagent.server.dto.PluginTokenRefreshRequest;
import com.jobagent.server.dto.PluginTokenRequest;
import com.jobagent.server.dto.PluginTokenResponse;
import com.jobagent.server.dto.PluginTokenRevokeRequest;
import com.jobagent.server.dto.StatusResponse;
import com.jobagent.server.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthRegisterResponse register(@Valid @RequestBody AuthRegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthLoginResponse login(@Valid @RequestBody AuthLoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/plugin/token")
    public PluginTokenResponse issuePluginToken(@Valid @RequestBody PluginTokenRequest request) {
        return authService.issuePluginToken(request);
    }

    @PostMapping("/plugin/refresh")
    public PluginTokenResponse refresh(@Valid @RequestBody PluginTokenRefreshRequest request) {
        return authService.refreshPluginToken(request);
    }

    @PostMapping("/plugin/revoke")
    public StatusResponse revoke(@Valid @RequestBody PluginTokenRevokeRequest request) {
        authService.revokePluginToken(request);
        return new StatusResponse("ok");
    }
}
