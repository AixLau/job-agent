package com.jobagent.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.server.dto.AuthLoginRequest;
import com.jobagent.server.dto.AuthLoginResponse;
import com.jobagent.server.dto.AuthRegisterRequest;
import com.jobagent.server.dto.AuthRegisterResponse;
import com.jobagent.server.dto.PluginTokenRefreshRequest;
import com.jobagent.server.dto.PluginTokenRequest;
import com.jobagent.server.dto.PluginTokenResponse;
import com.jobagent.server.dto.PluginTokenRevokeRequest;
import com.jobagent.server.repository.UserRepository;
import com.jobagent.server.store.UserEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final AuditService auditService;
    private final ObjectMapper mapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository,
                       TokenService tokenService,
                       AuditService auditService,
                       ObjectMapper mapper) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.auditService = auditService;
        this.mapper = mapper;
    }

    public AuthRegisterResponse register(AuthRegisterRequest request) {
        if (userRepository.findByAccount(request.account()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "account exists");
        }
        String id = UUID.randomUUID().toString();
        UserEntity entity = new UserEntity(
            id,
            request.account(),
            passwordEncoder.encode(request.password()),
            request.email(),
            "INCOMPLETE"
        );
        try {
            userRepository.save(entity);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "account exists");
        }
        auditService.record(entity.getId(), "AUTH_REGISTER", registerPayload(request));
        return new AuthRegisterResponse(new AuthRegisterResponse.UserInfo(entity.getId(), entity.getAccount()));
    }

    public AuthLoginResponse login(AuthLoginRequest request) {
        UserEntity entity = userRepository.findByAccount(request.account())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials"));
        if (!passwordEncoder.matches(request.password(), entity.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials");
        }
        TokenService.TokenPair pair = tokenService.issueTokens(entity.getId());
        auditService.record(entity.getId(), "AUTH_LOGIN", loginPayload(request));
        return new AuthLoginResponse(pair.accessToken(), pair.refreshToken(), pair.expiresIn());
    }

    public PluginTokenResponse issuePluginToken(PluginTokenRequest request) {
        String userId = tokenService.validateAccessToken(request.accessToken());
        PluginTokenResponse response = tokenService.issuePluginToken(userId, request.browserId());
        auditService.record(userId, "PLUGIN_TOKEN_ISSUE", pluginIssuePayload(request));
        return response;
    }

    public PluginTokenResponse refreshPluginToken(PluginTokenRefreshRequest request) {
        TokenService.PluginTokenResult result = tokenService.refreshPluginToken(request.pluginToken(), request.browserId());
        auditService.record(result.userId(), "PLUGIN_TOKEN_REFRESH", pluginRefreshPayload(request));
        return result.response();
    }

    public void revokePluginToken(PluginTokenRevokeRequest request) {
        String userId = tokenService.revokePluginToken(request.pluginToken(), request.browserId());
        auditService.record(userId, "PLUGIN_TOKEN_REVOKE", pluginRevokePayload(request));
    }

    public void verifyPluginToken(String pluginToken) {
        tokenService.validatePluginToken(pluginToken);
    }

    public String requireUserIdFromPluginToken(String pluginToken) {
        return tokenService.validatePluginToken(pluginToken);
    }

    public String requireUserId(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing authorization");
        }
        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid authorization");
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid authorization");
        }
        return tokenService.validateAccessToken(token);
    }

    private String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private String registerPayload(AuthRegisterRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("account", request.account());
        if (request.email() != null) {
            payload.put("email", request.email());
        }
        return toJson(payload);
    }

    private String loginPayload(AuthLoginRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("account", request.account());
        return toJson(payload);
    }

    private String pluginIssuePayload(PluginTokenRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("browser_id", request.browserId());
        return toJson(payload);
    }

    private String pluginRefreshPayload(PluginTokenRefreshRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("browser_id", request.browserId());
        return toJson(payload);
    }

    private String pluginRevokePayload(PluginTokenRevokeRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("browser_id", request.browserId());
        return toJson(payload);
    }
}
