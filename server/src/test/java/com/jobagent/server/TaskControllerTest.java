package com.jobagent.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.server.repository.AuditLogRepository;
import com.jobagent.server.repository.TaskRepository;
import com.jobagent.server.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JobAgentServerApplication.class)
@AutoConfigureMockMvc
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void create_task_returns_task_response() throws Exception {
        auditLogRepository.deleteAll();
        taskRepository.deleteAll();
        userRepository.deleteAll();

        String accessToken = registerAndLogin("alice");

        String body = mapper.writeValueAsString(Map.of(
            "title", "Backend Engineer",
            "city", "Shanghai",
            "salary", "20k-30k",
            "experience", "3y",
            "exclude", List.of("outsourcing"),
            "preferences", List.of("java"),
            "automation_level", "SEMI",
            "strategy_text", "focus on java backend"
        ));

        mockMvc.perform(post("/api/tasks")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.task.id").exists())
            .andExpect(jsonPath("$.task.status").value("ACTIVE"))
            .andExpect(jsonPath("$.task.title").value("Backend Engineer"))
            .andExpect(jsonPath("$.task.automation_level").value("SEMI"))
            .andExpect(jsonPath("$.task.strategy_json").value("{\"goal\":\"focus on java backend\"}"))
            .andExpect(jsonPath("$.task.created_at").exists());
    }

    @Test
    void list_tasks_returns_only_user_tasks() throws Exception {
        auditLogRepository.deleteAll();
        taskRepository.deleteAll();
        userRepository.deleteAll();

        String aliceToken = registerAndLogin("alice");
        String bobToken = registerAndLogin("bob");

        createTask(aliceToken, "Role A");
        createTask(aliceToken, "Role B");
        createTask(bobToken, "Role C");

        mockMvc.perform(get("/api/tasks")
                .header("Authorization", "Bearer " + aliceToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tasks.length()").value(2));
    }

    @Test
    void patch_task_updates_fields_and_strategy() throws Exception {
        auditLogRepository.deleteAll();
        taskRepository.deleteAll();
        userRepository.deleteAll();

        String accessToken = registerAndLogin("alice");

        String createResponse = createTask(accessToken, "Role A");
        Map<String, Object> createPayload = mapper.readValue(createResponse, new TypeReference<>() {});
        Map<String, Object> task = (Map<String, Object>) createPayload.get("task");
        String taskId = (String) task.get("id");

        String patchBody = mapper.writeValueAsString(Map.of(
            "title", "Role A Updated",
            "strategy_text", "new strategy"
        ));

        mockMvc.perform(patch("/api/tasks/" + taskId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(patchBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.task.title").value("Role A Updated"))
            .andExpect(jsonPath("$.task.strategy_json").value("{\"goal\":\"new strategy\"}"));
    }

    @Test
    void create_task_missing_authorization_returns_unauthorized() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
            "title", "Backend Engineer",
            "city", "Shanghai",
            "salary", "20k-30k",
            "experience", "3y",
            "exclude", List.of("outsourcing"),
            "preferences", List.of("java"),
            "automation_level", "SEMI",
            "strategy_text", "focus on java backend"
        ));

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void list_tasks_missing_authorization_returns_unauthorized() throws Exception {
        mockMvc.perform(get("/api/tasks"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void patch_task_missing_authorization_returns_unauthorized() throws Exception {
        String patchBody = mapper.writeValueAsString(Map.of(
            "title", "Role A Updated"
        ));

        mockMvc.perform(patch("/api/tasks/t-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(patchBody))
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

    private String createTask(String accessToken, String title) throws Exception {
        String body = mapper.writeValueAsString(Map.of(
            "title", title,
            "city", "Shanghai",
            "salary", "20k-30k",
            "experience", "3y",
            "exclude", List.of("outsourcing"),
            "preferences", List.of("java"),
            "automation_level", "SEMI",
            "strategy_text", "strategy for " + title
        ));

        return mockMvc.perform(post("/api/tasks")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    }
}
