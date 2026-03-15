package com.jobagent.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.server.dto.WorkerDraftResponse;
import com.jobagent.server.dto.WorkerJobMatchResponse;
import com.jobagent.server.dto.WorkerReplyClassifyResponse;
import com.jobagent.server.repository.ConversationRepository;
import com.jobagent.server.repository.DashboardRecommendationRepository;
import com.jobagent.server.repository.JobMatchRepository;
import com.jobagent.server.repository.JobPostRepository;
import com.jobagent.server.repository.MessageDraftRepository;
import com.jobagent.server.repository.MessageRepository;
import com.jobagent.server.repository.PluginTokenRepository;
import com.jobagent.server.repository.ResumeRepository;
import com.jobagent.server.repository.TaskRepository;
import com.jobagent.server.service.WorkerClient;
import com.jobagent.server.store.ConversationEntity;
import com.jobagent.server.store.JobMatchEntity;
import com.jobagent.server.store.JobPostEntity;
import com.jobagent.server.store.MessageDraftEntity;
import com.jobagent.server.store.PluginTokenEntity;
import com.jobagent.server.store.ResumeEntity;
import com.jobagent.server.store.TaskEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JobAgentServerApplication.class)
@AutoConfigureMockMvc
class PluginGatewayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private PluginTokenRepository pluginTokenRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private JobMatchRepository jobMatchRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private DashboardRecommendationRepository dashboardRecommendationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageDraftRepository messageDraftRepository;

    @MockBean
    private WorkerClient workerClient;

    @Test
    void plugin_token_required() throws Exception {
        mockMvc.perform(post("/plugin/heartbeat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("PLUGIN_TOKEN_INVALID"));
    }

    @Test
    void page_report_missing_fields_returns_validation_failed() throws Exception {
        resetData();
        seedTokenAndTask();

        String body = mapper.writeValueAsString(Map.of(
            "page_type", "detail",
            "raw_text", "raw",
            "source_url", "https://example.com/job/1",
            "dom_hash", "hash1"
        ));

        mockMvc.perform(post("/plugin/page/report")
                .header("X-Plugin-Token", "token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void page_report_ok_and_duplicate() throws Exception {
        resetData();
        seedTokenAndTask();

        when(workerClient.jobMatch(any()))
            .thenReturn(new WorkerJobMatchResponse(80, List.of("r1"), List.of("risk1"), null));
        when(workerClient.buildDraft(any()))
            .thenReturn(new WorkerDraftResponse("draft text"));

        String body = mapper.writeValueAsString(Map.of(
            "task_id", "task-1",
            "page_type", "detail",
            "raw_text", "raw",
            "extracted_json", Map.of(
                "source", "boss",
                "external_id", "ext-1",
                "title", "Role A",
                "company", "Company A"
            ),
            "source_url", "https://example.com/job/1",
            "dom_hash", "hash1",
            "want_draft", true
        ));

        mockMvc.perform(post("/plugin/page/report")
                .header("X-Plugin-Token", "token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.analysis.score").value(80))
            .andExpect(jsonPath("$.draft.content").value("draft text"));

        mockMvc.perform(post("/plugin/page/report")
                .header("X-Plugin-Token", "token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DUPLICATE_IGNORED"))
            .andExpect(jsonPath("$.payload.analysis.score").exists());
    }

    @Test
    void page_report_same_job_different_task_is_not_duplicate() throws Exception {
        resetData();
        seedTokenAndTask();
        taskRepository.save(new TaskEntity(
            "task-2",
            "user-1",
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

        when(workerClient.jobMatch(any()))
            .thenReturn(new WorkerJobMatchResponse(60, List.of("r1"), List.of("risk1"), null));

        String body = mapper.writeValueAsString(Map.of(
            "task_id", "task-1",
            "page_type", "list",
            "raw_text", "raw",
            "extracted_json", Map.of(
                "source", "boss",
                "external_id", "ext-1",
                "title", "Role A",
                "company", "Company A"
            ),
            "source_url", "https://example.com/job/1",
            "dom_hash", "hash1",
            "want_draft", false
        ));

        mockMvc.perform(post("/plugin/page/report")
                .header("X-Plugin-Token", "token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk());

        String bodySecondTask = mapper.writeValueAsString(Map.of(
            "task_id", "task-2",
            "page_type", "list",
            "raw_text", "raw",
            "extracted_json", Map.of(
                "source", "boss",
                "external_id", "ext-1",
                "title", "Role A",
                "company", "Company A"
            ),
            "source_url", "https://example.com/job/1",
            "dom_hash", "hash1",
            "want_draft", false
        ));

        mockMvc.perform(post("/plugin/page/report")
                .header("X-Plugin-Token", "token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodySecondTask))
            .andExpect(status().isOk());
    }

    @Test
    void page_report_skips_recommendation_and_draft_when_hard_filter_fails() throws Exception {
        resetData();
        seedTokenAndTask();
        taskRepository.save(new TaskEntity(
            "task-1",
            "user-1",
            "Role",
            "Shanghai",
            "20k-30k",
            "3-5年",
            "AUTO",
            "ACTIVE",
            "{\"goal\":\"x\"}",
            "{\"city\":\"上海\",\"salary\":\"20k-30k\",\"experience\":\"3-5年\",\"exclude\":[\"外包\"],\"preferences\":[\"B端\"],\"automationLevel\":\"AUTO\"}",
            "[]",
            "[\"B端\"]",
            Instant.now()
        ));

        when(workerClient.jobMatch(any()))
            .thenReturn(new WorkerJobMatchResponse(
                80,
                List.of("r1"),
                List.of(),
                Map.of("salary_min", 10000, "salary_max", 15000, "exp_min", 1, "exp_max", 2)
            ));
        when(workerClient.buildDraft(any()))
            .thenReturn(new WorkerDraftResponse("draft text"));

        String body = mapper.writeValueAsString(Map.of(
            "task_id", "task-1",
            "page_type", "detail",
            "raw_text", "北京 C端 外包",
            "extracted_json", Map.of(
                "source", "boss",
                "external_id", "ext-filtered",
                "title", "Role A",
                "company", "Company A",
                "city", "北京",
                "salary", "10k-15k",
                "experience", "1-2年"
            ),
            "source_url", "https://example.com/job/2",
            "dom_hash", "hash2",
            "want_draft", true
        ));

        mockMvc.perform(post("/plugin/page/report")
                .header("X-Plugin-Token", "token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.analysis.score").value(80))
            .andExpect(jsonPath("$.draft").doesNotExist());

        org.assertj.core.api.Assertions.assertThat(dashboardRecommendationRepository.count()).isEqualTo(0);
        org.assertj.core.api.Assertions.assertThat(messageDraftRepository.count()).isEqualTo(0);
    }

    @Test
    void chat_and_action_report_ok() throws Exception {
        resetData();
        seedTokenAndTask();

        ConversationEntity conversation = new ConversationEntity(
            UUID.randomUUID().toString(),
            "task-1",
            null,
            "conv-1",
            "NEW",
            null,
            null,
            null,
            null
        );
        conversationRepository.save(conversation);

        when(workerClient.replyClassify(any()))
            .thenReturn(new WorkerReplyClassifyResponse("INTERVIEW", "summary", "next"));

        String chatBody = mapper.writeValueAsString(Map.of(
            "task_id", "task-1",
            "conversation_id", "conv-1",
            "messages", List.of(Map.of("id", "m1", "role", "hr", "text", "hello")),
            "last_message_id", "m1"
        ));

        mockMvc.perform(post("/plugin/chat/report")
                .header("X-Plugin-Token", "token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(chatBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.reply.intent").value("INTERVIEW"))
            .andExpect(jsonPath("$.auto_send").value(false));

        String actionBody = mapper.writeValueAsString(Map.of(
            "task_id", "task-1",
            "action_type", "send_message",
            "status", "success",
            "payload", Map.of("conversation_id", "conv-1")
        ));

        mockMvc.perform(post("/plugin/action/report")
                .header("X-Plugin-Token", "token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(actionBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void action_report_delivered_updates_status() throws Exception {
        resetData();
        seedTokenAndTask();

        ConversationEntity conversation = new ConversationEntity(
            UUID.randomUUID().toString(),
            "task-1",
            null,
            "conv-2",
            "NEW",
            null,
            null,
            null,
            null
        );
        conversationRepository.save(conversation);

        String actionBody = mapper.writeValueAsString(Map.of(
            "task_id", "task-1",
            "action_type", "DELIVERED",
            "status", "success",
            "payload", Map.of("conversation_id", "conv-2")
        ));

        mockMvc.perform(post("/plugin/action/report")
                .header("X-Plugin-Token", "token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(actionBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));

        ConversationEntity updated = conversationRepository.findById(conversation.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getStatus()).isEqualTo("WAITING_HR");
    }

    @Test
    void chat_report_returns_auto_send_hint_when_allowed() throws Exception {
        resetData();
        seedTokenAndTask("AUTO");

        JobPostEntity jobPost = new JobPostEntity(
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
            "DRAFTED",
            Instant.now()
        );
        jobPostRepository.save(jobPost);
        jobMatchRepository.save(new JobMatchEntity(
            "match-1",
            "task-1",
            "job-1",
            80,
            "[\"fit\"]",
            "[]",
            "{\"automation_action\":\"AUTO_SEND\"}",
            Instant.now()
        ));

        ConversationEntity conversation = new ConversationEntity(
            UUID.randomUUID().toString(),
            "task-1",
            "job-1",
            "conv-auto",
            "WAITING_USER",
            null,
            null,
            null,
            null
        );
        conversationRepository.save(conversation);
        messageDraftRepository.save(new MessageDraftEntity(
            "draft-auto",
            conversation.getId(),
            "自动发送草稿",
            "SYSTEM",
            false,
            Instant.now()
        ));

        when(workerClient.replyClassify(any()))
            .thenReturn(new WorkerReplyClassifyResponse("FOLLOW_UP", "summary", "next"));

        String chatBody = mapper.writeValueAsString(Map.of(
            "task_id", "task-1",
            "conversation_id", "conv-auto",
            "messages", List.of(Map.of("id", "m1", "role", "hr", "text", "hello")),
            "last_message_id", "m1"
        ));

        mockMvc.perform(post("/plugin/chat/report")
                .header("X-Plugin-Token", "token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(chatBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.auto_send").value(true))
            .andExpect(jsonPath("$.draft.content").value("自动发送草稿"));
    }

    @Test
    void chat_report_auto_send_false_for_high_risk_or_non_auto() throws Exception {
        resetData();
        seedTokenAndTask("SEMI");

        JobPostEntity jobPost = new JobPostEntity(
            "job-2",
            "task-1",
            "boss",
            "ext-2",
            "Role B",
            "Company B",
            "Shanghai",
            "20k-30k",
            "3y",
            "raw",
            "{}",
            "DRAFTED",
            Instant.now()
        );
        jobPostRepository.save(jobPost);
        jobMatchRepository.save(new JobMatchEntity(
            "match-2",
            "task-1",
            "job-2",
            70,
            "[\"fit\"]",
            "[\"外包\"]",
            "{\"automation_action\":\"NEED_CONFIRM\"}",
            Instant.now()
        ));

        ConversationEntity conversation = new ConversationEntity(
            UUID.randomUUID().toString(),
            "task-1",
            "job-2",
            "conv-manual",
            "WAITING_USER",
            null,
            null,
            null,
            null
        );
        conversationRepository.save(conversation);
        messageDraftRepository.save(new MessageDraftEntity(
            "draft-manual",
            conversation.getId(),
            "人工确认草稿",
            "SYSTEM",
            false,
            Instant.now()
        ));

        when(workerClient.replyClassify(any()))
            .thenReturn(new WorkerReplyClassifyResponse("FOLLOW_UP", "summary", "next"));

        String chatBody = mapper.writeValueAsString(Map.of(
            "task_id", "task-1",
            "conversation_id", "conv-manual",
            "messages", List.of(Map.of("id", "m1", "role", "hr", "text", "hello")),
            "last_message_id", "m1"
        ));

        mockMvc.perform(post("/plugin/chat/report")
                .header("X-Plugin-Token", "token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(chatBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.auto_send").value(false))
            .andExpect(jsonPath("$.draft.content").value("人工确认草稿"));
    }

    @Test
    void chat_and_action_report_task_not_owned_returns_bad_request() throws Exception {
        resetData();
        seedTokenOnly();

        taskRepository.save(new TaskEntity(
            "task-2",
            "user-2",
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

        String chatBody = mapper.writeValueAsString(Map.of(
            "task_id", "task-2",
            "conversation_id", "conv-1",
            "messages", List.of(Map.of("id", "m1", "role", "hr", "text", "hello")),
            "last_message_id", "m1"
        ));

        mockMvc.perform(post("/plugin/chat/report")
                .header("X-Plugin-Token", "token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(chatBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("task not found"));

        String actionBody = mapper.writeValueAsString(Map.of(
            "task_id", "task-2",
            "action_type", "send_message",
            "status", "success",
            "payload", Map.of("conversation_id", "conv-1")
        ));

        mockMvc.perform(post("/plugin/action/report")
                .header("X-Plugin-Token", "token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(actionBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("task not found"));
    }

    private void resetData() {
        messageDraftRepository.deleteAll();
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        jobMatchRepository.deleteAll();
        dashboardRecommendationRepository.deleteAll();
        jobPostRepository.deleteAll();
        resumeRepository.deleteAll();
        taskRepository.deleteAll();
        pluginTokenRepository.deleteAll();
    }

    private void seedTokenAndTask() {
        seedTokenAndTask("SEMI");
    }

    private void seedTokenAndTask(String automationLevel) {
        pluginTokenRepository.save(new PluginTokenEntity(
            UUID.randomUUID().toString(),
            "user-1",
            "browser-1",
            "token",
            Instant.now().plusSeconds(3600),
            false
        ));
        taskRepository.save(new TaskEntity(
            "task-1",
            "user-1",
            "Role",
            "Shanghai",
            "20k-30k",
            "3y",
            automationLevel,
            "ACTIVE",
            "{\"goal\":\"x\"}",
            "{}",
            "[]",
            "[]",
            Instant.now()
        ));
        resumeRepository.save(new ResumeEntity(
            "resume-1",
            "user-1",
            "content",
            "{\"raw\":\"content\"}"
        ));
    }

    private void seedTokenOnly() {
        pluginTokenRepository.save(new PluginTokenEntity(
            UUID.randomUUID().toString(),
            "user-1",
            "browser-1",
            "token",
            Instant.now().plusSeconds(3600),
            false
        ));
    }
}
