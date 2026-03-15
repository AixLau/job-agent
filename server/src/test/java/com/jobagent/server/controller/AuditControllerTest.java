package com.jobagent.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.server.JobAgentServerApplication;
import com.jobagent.server.repository.AuditLogRepository;
import com.jobagent.server.repository.UserRepository;
import com.jobagent.server.store.AuditLogEntity;
import com.jobagent.server.store.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JobAgentServerApplication.class)
@AutoConfigureMockMvc
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void setup() {
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void list_audits_returns_paginated_items() throws Exception {
        String token = registerAndLogin("alice");
        UserEntity user = userRepository.findByAccount("alice").orElseThrow();
        auditLogRepository.deleteAll();
        auditLogRepository.save(new AuditLogEntity(UUID.randomUUID().toString(), user.getId(), "TASK_CREATE", "{\"task_id\":\"t1\"}"));
        Thread.sleep(5);
        auditLogRepository.save(new AuditLogEntity(UUID.randomUUID().toString(), user.getId(), "JOB_FOLLOW", "{\"job_post_id\":\"j1\"}"));

        String response = mockMvc.perform(get("/api/audits?page=0&size=10")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Map<String, Object> payload = mapper.readValue(response, new TypeReference<>() {});
        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
        assertThat(items).hasSize(2);
        assertThat(payload.get("page")).isEqualTo(0);
        assertThat(payload.get("size")).isEqualTo(10);
        assertThat(payload.get("total")).isEqualTo(2);
        assertThat(items.get(0).get("action_type")).isEqualTo("JOB_FOLLOW");
        assertThat(items.get(0)).containsKeys("created_at", "payload", "result", "model_output", "risk_tags");
    }

    @Test
    void audits_requires_auth() throws Exception {
        mockMvc.perform(get("/api/audits?page=0&size=10"))
            .andExpect(status().isUnauthorized());
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
