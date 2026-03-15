package com.jobagent.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.server.JobAgentServerApplication;
import com.jobagent.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JobAgentServerApplication.class)
@AutoConfigureMockMvc
class SettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
    }

    @Test
    void get_and_save_settings_round_trip() throws Exception {
        String token = registerAndLogin("alice");

        String initialResponse = mockMvc.perform(get("/api/settings")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Map<String, Object> initialPayload = mapper.readValue(initialResponse, new TypeReference<>() {});
        Map<String, Object> initialSettings = (Map<String, Object>) initialPayload.get("settings");
        assertThat(initialSettings.get("default_automation_level")).isEqualTo("SEMI");
        assertThat(initialSettings.get("high_risk_requires_review")).isEqualTo(true);

        String saveBody = mapper.writeValueAsString(Map.of(
            "default_automation_level", "AUTO",
            "auto_send_enabled", true,
            "high_risk_requires_review", true,
            "chat_immediate_auto_send", true,
            "daily_action_limit", 80
        ));

        String saveResponse = mockMvc.perform(post("/api/settings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(saveBody))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Map<String, Object> savePayload = mapper.readValue(saveResponse, new TypeReference<>() {});
        Map<String, Object> settings = (Map<String, Object>) savePayload.get("settings");
        assertThat(settings.get("default_automation_level")).isEqualTo("AUTO");
        assertThat(settings.get("auto_send_enabled")).isEqualTo(true);
        assertThat(settings.get("chat_immediate_auto_send")).isEqualTo(true);
        assertThat(settings.get("daily_action_limit")).isEqualTo(80);
    }

    private String registerAndLogin(String account) throws Exception {
        String registerBody = mapper.writeValueAsString(Map.of(
            "account", account,
            "password", "pwd123",
            "email", account + "@example.com"
        ));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody))
            .andExpect(status().isOk());

        String loginBody = mapper.writeValueAsString(Map.of(
            "account", account,
            "password", "pwd123"
        ));

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Map<String, Object> loginPayload = mapper.readValue(loginResponse, new TypeReference<>() {});
        return (String) loginPayload.get("access_token");
    }
}
