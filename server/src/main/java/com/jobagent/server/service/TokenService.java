package com.jobagent.server.service;

import com.jobagent.server.dto.PluginTokenResponse;
import com.jobagent.server.repository.PluginTokenRepository;
import com.jobagent.server.store.PluginTokenEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {

    private static final long ACCESS_TTL_SECONDS = 3600;
    private static final long REFRESH_TTL_SECONDS = 7 * 24 * 3600;
    private static final long PLUGIN_TTL_SECONDS = 86400;

    private final PluginTokenRepository pluginTokenRepository;
    private final Map<String, TokenRecord> accessTokens = new ConcurrentHashMap<>();
    private final Map<String, TokenRecord> refreshTokens = new ConcurrentHashMap<>();

    public TokenService(PluginTokenRepository pluginTokenRepository) {
        this.pluginTokenRepository = pluginTokenRepository;
    }

    public record TokenPair(String accessToken, String refreshToken, long expiresIn) {
    }

    public record PluginTokenResult(PluginTokenResponse response, String userId) {
    }

    public TokenPair issueTokens(String userId) {
        Instant now = Instant.now();
        String accessToken = UUID.randomUUID().toString();
        String refreshToken = UUID.randomUUID().toString();
        accessTokens.put(accessToken, new TokenRecord(userId, now.plusSeconds(ACCESS_TTL_SECONDS)));
        refreshTokens.put(refreshToken, new TokenRecord(userId, now.plusSeconds(REFRESH_TTL_SECONDS)));
        return new TokenPair(accessToken, refreshToken, ACCESS_TTL_SECONDS);
    }

    public String validateAccessToken(String accessToken) {
        TokenRecord record = accessTokens.get(accessToken);
        if (record == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid access_token");
        }
        if (record.expiresAt().isBefore(Instant.now())) {
            accessTokens.remove(accessToken);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "access_token expired");
        }
        return record.userId();
    }

    public PluginTokenResponse issuePluginToken(String userId, String browserId) {
        Instant expiresAt = Instant.now().plusSeconds(PLUGIN_TTL_SECONDS);
        String token = UUID.randomUUID().toString();
        PluginTokenEntity entity = new PluginTokenEntity(
            UUID.randomUUID().toString(),
            userId,
            browserId,
            token,
            expiresAt,
            false
        );
        pluginTokenRepository.save(entity);
        return new PluginTokenResponse(token, PLUGIN_TTL_SECONDS);
    }

    public PluginTokenResult refreshPluginToken(String token, String browserId) {
        PluginTokenEntity entity = pluginTokenRepository.findByToken(token)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid plugin_token"));
        ensureBrowserMatch(entity, browserId);
        ensureActive(entity);
        entity.setExpiresAt(Instant.now().plusSeconds(PLUGIN_TTL_SECONDS));
        PluginTokenEntity saved = pluginTokenRepository.save(entity);
        return new PluginTokenResult(
            new PluginTokenResponse(saved.getToken(), PLUGIN_TTL_SECONDS),
            saved.getUserId()
        );
    }

    public String revokePluginToken(String token, String browserId) {
        PluginTokenEntity entity = pluginTokenRepository.findByToken(token)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid plugin_token"));
        ensureBrowserMatch(entity, browserId);
        entity.setRevoked(true);
        PluginTokenEntity saved = pluginTokenRepository.save(entity);
        return saved.getUserId();
    }

    public String validatePluginToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "PLUGIN_TOKEN_INVALID");
        }
        PluginTokenEntity entity = pluginTokenRepository.findByToken(token)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "PLUGIN_TOKEN_INVALID"));
        try {
            ensureActive(entity);
        } catch (ResponseStatusException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "PLUGIN_TOKEN_INVALID");
        }
        return entity.getUserId();
    }

    private void ensureBrowserMatch(PluginTokenEntity entity, String browserId) {
        if (browserId == null || !browserId.equals(entity.getBrowserId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "browser_id mismatch");
        }
    }

    private void ensureActive(PluginTokenEntity entity) {
        if (entity.isRevoked()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "plugin_token revoked");
        }
        if (entity.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "plugin_token expired");
        }
    }

    private record TokenRecord(String userId, Instant expiresAt) {
    }
}
