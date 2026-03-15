package com.jobagent.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.server.dto.DraftItem;
import com.jobagent.server.dto.RecommendationItem;
import com.jobagent.server.dto.ReplyItem;
import com.jobagent.server.repository.DashboardDraftRepository;
import com.jobagent.server.repository.DashboardRecommendationRepository;
import com.jobagent.server.repository.DashboardReplyRepository;
import com.jobagent.server.repository.ConversationRepository;
import com.jobagent.server.repository.JobPostRepository;
import com.jobagent.server.repository.UserRepository;
import com.jobagent.server.store.DashboardStore;
import com.jobagent.server.store.ConversationEntity;
import com.jobagent.server.store.JobPostEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JobAgentServerApplication.class)
@AutoConfigureMockMvc
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private DashboardStore dashboardStore;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DashboardRecommendationRepository recommendationRepository;

    @Autowired
    private DashboardDraftRepository draftRepository;

    @Autowired
    private DashboardReplyRepository replyRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Test
    void dashboard_snapshot_returns_metrics_and_lists() throws Exception {
        resetData();
        String accessToken = registerAndLogin();
        String userId = userRepository.findByAccount("alice").orElseThrow().getId();

        jobPostRepository.save(new JobPostEntity(
            "job-1",
            "task-1",
            "boss",
            "ext-1",
            "Role A",
            "Company A",
            "Shanghai",
            "20k-30k",
            "3y",
            "raw",
            "{}",
            "ACTIVE",
            Instant.parse("2024-01-01T00:00:00Z")
        ));
        conversationRepository.save(new ConversationEntity(
            "conv-1",
            "task-1",
            "job-1",
            "ext-conv-1",
            "NEW",
            null,
            null,
            null,
            Instant.parse("2024-01-02T00:00:00Z")
        ));

        dashboardStore.addRecommendation(userId, new RecommendationItem(
            "job-1",
            "Role A",
            "Company A",
            80,
            List.of("risk1"),
            "SHORTLISTED"
        ));
        dashboardStore.addDraft(userId, new DraftItem(
            "draft-1",
            "conv-1",
            "hello",
            Instant.parse("2024-01-01T00:00:00Z"),
            false
        ));
        dashboardStore.addReply(userId, new ReplyItem(
            "conv-1",
            "summary",
            "INTERVIEW",
            Instant.parse("2024-01-02T00:00:00Z")
        ));

        mockMvc.perform(get("/api/dashboard")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metrics.recommendations").value(1))
            .andExpect(jsonPath("$.metrics.drafts").value(1))
            .andExpect(jsonPath("$.metrics.replies").value(1))
            .andExpect(jsonPath("$.metrics.interviews").value(1))
            .andExpect(jsonPath("$.recommendations[0].job_post_id").value("job-1"))
            .andExpect(jsonPath("$.drafts[0].draft_id").value("draft-1"))
            .andExpect(jsonPath("$.drafts[0].job_post_id").value("job-1"))
            .andExpect(jsonPath("$.drafts[0].company").value("Company A"))
            .andExpect(jsonPath("$.replies[0].conversation_id").value("conv-1"))
            .andExpect(jsonPath("$.replies[0].job_post_id").value("job-1"))
            .andExpect(jsonPath("$.replies[0].company").value("Company A"))
            .andExpect(jsonPath("$.interviews[0].conversation_id").value("conv-1"))
            .andExpect(jsonPath("$.updated_at").exists());
    }

    private void resetData() {
        conversationRepository.deleteAll();
        jobPostRepository.deleteAll();
        replyRepository.deleteAll();
        draftRepository.deleteAll();
        recommendationRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String registerAndLogin() throws Exception {
        String registerBody = mapper.writeValueAsString(Map.of(
            "account", "alice",
            "password", "pwd123",
            "email", "a@x.com"
        ));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody))
            .andExpect(status().isOk());

        String loginBody = mapper.writeValueAsString(Map.of(
            "account", "alice",
            "password", "pwd123"
        ));

        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Map<String, Object> payload = mapper.readValue(response, new TypeReference<>() {});
        return (String) payload.get("access_token");
    }
}
