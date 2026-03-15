package com.jobagent.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.server.repository.ConversationRepository;
import com.jobagent.server.repository.DashboardDraftRepository;
import com.jobagent.server.repository.MessageDraftRepository;
import com.jobagent.server.repository.TaskRepository;
import com.jobagent.server.repository.UserRepository;
import com.jobagent.server.store.ConversationEntity;
import com.jobagent.server.store.DashboardDraftEntity;
import com.jobagent.server.store.MessageDraftEntity;
import com.jobagent.server.store.TaskEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JobAgentServerApplication.class)
@AutoConfigureMockMvc
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageDraftRepository messageDraftRepository;

    @Autowired
    private DashboardDraftRepository dashboardDraftRepository;

    @Test
    void approve_reject_close_regenerate_flow() throws Exception {
        resetData();
        String token = registerAndLogin();

        taskRepository.save(new TaskEntity(
            "task-1",
            userRepository.findByAccount("alice").orElseThrow().getId(),
            "Role",
            "Shanghai",
            "20k-30k",
            "3y",
            "SEMI",
            "ACTIVE",
            "{\"goal\":\"x\"}",
            "{}",
            "[]",
            "[]",
            Instant.now()
        ));

        ConversationEntity conversation = new ConversationEntity(
            "conv-1",
            "task-1",
            null,
            "ext-1",
            "NEW",
            null,
            null,
            null,
            Instant.now()
        );
        conversationRepository.save(conversation);

        MessageDraftEntity draft = new MessageDraftEntity(
            "draft-1",
            conversation.getId(),
            "draft text",
            "SYSTEM",
            false,
            Instant.now()
        );
        messageDraftRepository.save(draft);
        dashboardDraftRepository.save(new DashboardDraftEntity(
            "draft-1",
            userRepository.findByAccount("alice").orElseThrow().getId(),
            conversation.getId(),
            "draft text",
            false,
            Instant.now()
        ));

        String approveBody = mapper.writeValueAsString(Map.of("action", "fill"));
        mockMvc.perform(post("/api/drafts/draft-1/approve")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(approveBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.draft.approved").value(true))
            .andExpect(jsonPath("$.action_hint.fill_content").value("draft text"));

        MessageDraftEntity approved = messageDraftRepository.findById("draft-1").orElseThrow();
        assertThat(approved.isApproved()).isTrue();
        DashboardDraftEntity approvedDashboard = dashboardDraftRepository.findById("draft-1").orElseThrow();
        assertThat(approvedDashboard.isApproved()).isTrue();

        String rejectBody = mapper.writeValueAsString(Map.of("reason", "not good"));
        mockMvc.perform(post("/api/drafts/draft-1/reject")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(rejectBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.draft.approved").value(false));

        MessageDraftEntity rejected = messageDraftRepository.findById("draft-1").orElseThrow();
        assertThat(rejected.isApproved()).isFalse();
        DashboardDraftEntity rejectedDashboard = dashboardDraftRepository.findById("draft-1").orElseThrow();
        assertThat(rejectedDashboard.isApproved()).isFalse();

        String closeBody = mapper.writeValueAsString(Map.of("reason", "done"));
        mockMvc.perform(post("/api/conversations/conv-1/close")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(closeBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.conversation.status").value("CLOSED"));

        String regenerateBody = mapper.writeValueAsString(Map.of("style", "formal"));
        mockMvc.perform(post("/api/conversations/conv-1/regenerate")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(regenerateBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.draft.content").value("draft text"));
    }

    private void resetData() {
        dashboardDraftRepository.deleteAll();
        messageDraftRepository.deleteAll();
        conversationRepository.deleteAll();
        taskRepository.deleteAll();
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
