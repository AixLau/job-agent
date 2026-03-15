package com.jobagent.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.server.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JobAgentServerApplication.class)
@AutoConfigureMockMvc
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void fetch_profile_returns_default_payload_when_profile_absent() throws Exception {
        userRepository.deleteAll();
        String token = registerAndLogin("alice");

        mockMvc.perform(get("/api/profile")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.profile.account").value("alice"))
            .andExpect(jsonPath("$.profile.email").value("alice@example.com"))
            .andExpect(jsonPath("$.profile.full_name").value(""))
            .andExpect(jsonPath("$.profile.skills.length()").value(0))
            .andExpect(jsonPath("$.profile.profile_status").value("INCOMPLETE"));
    }

    @Test
    void save_profile_creates_or_updates_profile_details() throws Exception {
        userRepository.deleteAll();
        String token = registerAndLogin("alice");

        String body = mapper.writeValueAsString(Map.of(
            "full_name", "Alice Zhang",
            "phone", "13800138000",
            "city", "Shanghai",
            "years_experience", 5,
            "summary", "B端产品经理",
            "skills", List.of("PRD", "Growth")
        ));

        mockMvc.perform(post("/api/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.profile.full_name").value("Alice Zhang"))
            .andExpect(jsonPath("$.profile.phone").value("13800138000"))
            .andExpect(jsonPath("$.profile.city").value("Shanghai"))
            .andExpect(jsonPath("$.profile.years_experience").value(5))
            .andExpect(jsonPath("$.profile.skills[0]").value("PRD"))
            .andExpect(jsonPath("$.profile.profile_status").value("COMPLETE"));

        mockMvc.perform(get("/api/profile")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.profile.full_name").value("Alice Zhang"))
            .andExpect(jsonPath("$.profile.skills[1]").value("Growth"));
    }

    @Test
    void profile_requires_authorization() throws Exception {
        mockMvc.perform(get("/api/profile"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
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
