package com.jobagent.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.server.JobAgentServerApplication;
import com.jobagent.server.repository.JobPostRepository;
import com.jobagent.server.repository.TaskRepository;
import com.jobagent.server.repository.UserCompanyBlacklistRepository;
import com.jobagent.server.repository.UserJobActionRepository;
import com.jobagent.server.repository.UserRepository;
import com.jobagent.server.store.JobPostEntity;
import com.jobagent.server.store.TaskEntity;
import com.jobagent.server.store.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JobAgentServerApplication.class)
@AutoConfigureMockMvc
class JobActionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private UserJobActionRepository userJobActionRepository;

    @Autowired
    private UserCompanyBlacklistRepository userCompanyBlacklistRepository;

    @BeforeEach
    void setup() {
        userJobActionRepository.deleteAll();
        userCompanyBlacklistRepository.deleteAll();
        jobPostRepository.deleteAll();
        taskRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void follow_creates_action_and_returns_follow_item() throws Exception {
        String token = registerAndLogin("alice");
        UserEntity user = userRepository.findByAccount("alice").orElseThrow();
        TaskEntity task = createTask(user.getId());
        JobPostEntity post = createJobPost(task.getId(), "Boss77", "Company A");

        mockMvc.perform(post("/api/jobs/" + post.getId() + "/follow")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.follow_item.job_post_id").value(post.getId()))
            .andExpect(jsonPath("$.follow_item.title").value("Boss77"))
            .andExpect(jsonPath("$.follow_item.company").value("Company A"))
            .andExpect(jsonPath("$.follow_item.created_at").exists());

        var action = userJobActionRepository.findByUserIdAndJobPostId(user.getId(), post.getId()).orElseThrow();
        assertThat(action.getActionType()).isEqualTo("FOLLOW");
    }

    @Test
    void follow_is_idempotent() throws Exception {
        String token = registerAndLogin("alice");
        UserEntity user = userRepository.findByAccount("alice").orElseThrow();
        TaskEntity task = createTask(user.getId());
        JobPostEntity post = createJobPost(task.getId(), "Boss1", "Company A");

        mockMvc.perform(post("/api/jobs/" + post.getId() + "/follow")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/jobs/" + post.getId() + "/follow")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        assertThat(userJobActionRepository.count()).isEqualTo(1);
    }

    @Test
    void follow_list_is_paginated_and_ordered() throws Exception {
        String token = registerAndLogin("alice");
        UserEntity user = userRepository.findByAccount("alice").orElseThrow();
        TaskEntity task = createTask(user.getId());
        JobPostEntity postA = createJobPost(task.getId(), "BossA", "Company A");
        JobPostEntity postB = createJobPost(task.getId(), "BossB", "Company B");
        JobPostEntity postC = createJobPost(task.getId(), "BossC", "Company C");

        mockMvc.perform(post("/api/jobs/" + postA.getId() + "/follow")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
        Thread.sleep(5);
        mockMvc.perform(post("/api/jobs/" + postB.getId() + "/follow")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
        Thread.sleep(5);
        mockMvc.perform(post("/api/jobs/" + postC.getId() + "/follow")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        String response = mockMvc.perform(get("/api/follows?page=0&size=2")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Map<String, Object> payload = mapper.readValue(response, new TypeReference<>() {});
        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
        assertThat(items).hasSize(2);
        assertThat(payload.get("total")).isEqualTo(3);
        assertThat(payload.get("page")).isEqualTo(0);
        assertThat(payload.get("size")).isEqualTo(2);

        Instant first = Instant.parse((String) items.get(0).get("created_at"));
        Instant second = Instant.parse((String) items.get(1).get("created_at"));
        assertThat(first).isAfterOrEqualTo(second);
    }

    @Test
    void ignore_is_idempotent_and_archives_job() throws Exception {
        String token = registerAndLogin("alice");
        UserEntity user = userRepository.findByAccount("alice").orElseThrow();
        TaskEntity task = createTask(user.getId());
        JobPostEntity post = createJobPost(task.getId(), "BossIgnore", "Company A");

        mockMvc.perform(post("/api/jobs/" + post.getId() + "/ignore")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));

        mockMvc.perform(post("/api/jobs/" + post.getId() + "/ignore")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        JobPostEntity updated = jobPostRepository.findById(post.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("ARCHIVED");
        var action = userJobActionRepository.findByUserIdAndJobPostId(user.getId(), post.getId()).orElseThrow();
        assertThat(action.getActionType()).isEqualTo("IGNORE");
        assertThat(userJobActionRepository.count()).isEqualTo(1);
    }

    @Test
    void blacklist_requires_fields_and_is_idempotent() throws Exception {
        String token = registerAndLogin("alice");

        String emptyBody = mapper.writeValueAsString(Map.of());
        mockMvc.perform(post("/api/blacklist/company")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(emptyBody))
            .andExpect(status().isBadRequest());

        String onlyCompany = mapper.writeValueAsString(Map.of("company_name", "BadCo"));
        mockMvc.perform(post("/api/blacklist/company")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(onlyCompany))
            .andExpect(status().isBadRequest());

        String body = mapper.writeValueAsString(Map.of(
            "company_name", "BadCo",
            "source", "boss"
        ));

        mockMvc.perform(post("/api/blacklist/company")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));

        mockMvc.perform(post("/api/blacklist/company")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk());

        assertThat(userCompanyBlacklistRepository.count()).isEqualTo(1);
    }

    @Test
    void auth_required_for_job_actions() throws Exception {
        mockMvc.perform(post("/api/jobs/job-1/follow"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/jobs/job-1/ignore"))
            .andExpect(status().isUnauthorized());

        String body = mapper.writeValueAsString(Map.of(
            "company_name", "BadCo",
            "source", "boss"
        ));
        mockMvc.perform(post("/api/blacklist/company")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/follows"))
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

    private TaskEntity createTask(String userId) {
        TaskEntity task = new TaskEntity(
            UUID.randomUUID().toString(),
            userId,
            "Role",
            "Shanghai",
            "20k",
            "3y",
            "AUTO",
            "ACTIVE",
            "{}",
            "{}",
            "[]",
            "[]",
            Instant.now()
        );
        return taskRepository.save(task);
    }

    private JobPostEntity createJobPost(String taskId, String title, String company) {
        JobPostEntity post = new JobPostEntity(
            UUID.randomUUID().toString(),
            taskId,
            "boss",
            UUID.randomUUID().toString(),
            title,
            company,
            "Shanghai",
            "20k",
            "3y",
            "JD",
            "{}",
            "DISCOVERED",
            Instant.now()
        );
        return jobPostRepository.save(post);
    }
}
