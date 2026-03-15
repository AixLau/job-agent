package com.jobagent.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.server.repository.AuditLogRepository;
import com.jobagent.server.repository.PluginTokenRepository;
import com.jobagent.server.repository.UserRepository;
import com.jobagent.server.store.AuditLogEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JobAgentServerApplication.class)
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PluginTokenRepository pluginTokenRepository;

    @Test
    void auth_flow_register_login_refresh_revoke() throws Exception {
        auditLogRepository.deleteAll();
        pluginTokenRepository.deleteAll();
        userRepository.deleteAll();

        String registerBody = mapper.writeValueAsString(Map.of(
            "account", "alice",
            "password", "pwd123",
            "email", "a@x.com"
        ));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.id").exists())
            .andExpect(jsonPath("$.user.account").value("alice"));

        var user = userRepository.findByAccount("alice").orElseThrow();
        assertThat(user.getProfileStatus()).isEqualTo("INCOMPLETE");
        assertThat(user.getPasswordHash()).isNotEqualTo("pwd123");
        assertThat(user.getPasswordHash()).startsWith("$2");

        String loginBody = mapper.writeValueAsString(Map.of(
            "account", "alice",
            "password", "pwd123"
        ));

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").exists())
            .andExpect(jsonPath("$.refresh_token").exists())
            .andExpect(jsonPath("$.expires_in").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Map<String, Object> loginPayload = mapper.readValue(loginResponse, new TypeReference<>() {});
        String accessToken = (String) loginPayload.get("access_token");

        String pluginBody = mapper.writeValueAsString(Map.of(
            "access_token", accessToken,
            "browser_id", "chrome-1"
        ));

        String pluginResponse = mockMvc.perform(post("/api/auth/plugin/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pluginBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.plugin_token").exists())
            .andExpect(jsonPath("$.expires_in").value(86400))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Map<String, Object> pluginPayload = mapper.readValue(pluginResponse, new TypeReference<>() {});
        String pluginToken = (String) pluginPayload.get("plugin_token");

        String refreshMismatchBody = mapper.writeValueAsString(Map.of(
            "plugin_token", pluginToken,
            "browser_id", "chrome-2"
        ));

        mockMvc.perform(post("/api/auth/plugin/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshMismatchBody))
            .andExpect(status().isUnauthorized());

        String refreshBody = mapper.writeValueAsString(Map.of(
            "plugin_token", pluginToken,
            "browser_id", "chrome-1"
        ));

        mockMvc.perform(post("/api/auth/plugin/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.plugin_token").exists())
            .andExpect(jsonPath("$.expires_in").value(86400));

        String revokeMismatchBody = mapper.writeValueAsString(Map.of(
            "plugin_token", pluginToken,
            "browser_id", "chrome-2"
        ));

        mockMvc.perform(post("/api/auth/plugin/revoke")
                .contentType(MediaType.APPLICATION_JSON)
                .content(revokeMismatchBody))
            .andExpect(status().isUnauthorized());

        String revokeBody = mapper.writeValueAsString(Map.of(
            "plugin_token", pluginToken,
            "browser_id", "chrome-1"
        ));

        mockMvc.perform(post("/api/auth/plugin/revoke")
                .contentType(MediaType.APPLICATION_JSON)
                .content(revokeBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));

        List<String> payloads = auditLogRepository.findAll()
            .stream()
            .map(AuditLogEntity::getPayload)
            .toList();

        assertThat(payloads).hasSize(5);
        assertThat(payloads).allMatch(payload -> payload == null || !payload.contains("pwd123"));
        assertThat(payloads).allMatch(payload -> payload == null || !payload.contains(accessToken));
        assertThat(payloads).allMatch(payload -> payload == null || !payload.contains(pluginToken));
        assertThat(payloads).anyMatch(payload -> payload != null && payload.contains("browser_id"));
        assertThat(payloads).anyMatch(payload -> payload != null && payload.contains("alice"));
    }

    @Test
    void register_missing_account_returnsBadRequest() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
            "password", "pwd123",
            "email", "a@x.com"
        ));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void register_blank_account_returnsBadRequest() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
            "account", "   ",
            "password", "pwd123"
        ));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void register_missing_password_returnsBadRequest() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
            "account", "alice"
        ));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void login_missing_account_returnsBadRequest() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
            "password", "pwd123"
        ));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void login_missing_password_returnsBadRequest() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
            "account", "alice"
        ));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void plugin_token_missing_browser_id_returnsBadRequest() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
            "access_token", "access-token"
        ));

        mockMvc.perform(post("/api/auth/plugin/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void plugin_refresh_missing_browser_id_returnsBadRequest() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
            "plugin_token", "plugin-token"
        ));

        mockMvc.perform(post("/api/auth/plugin/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void plugin_revoke_missing_browser_id_returnsBadRequest() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
            "plugin_token", "plugin-token"
        ));

        mockMvc.perform(post("/api/auth/plugin/revoke")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }
}
