package com.jobagent.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.server.dto.WorkerDraftResponse;
import com.jobagent.server.dto.WorkerJobMatchResponse;
import com.jobagent.server.dto.WorkerReplyClassifyResponse;
import com.jobagent.server.repository.ConversationRepository;
import com.jobagent.server.repository.JobMatchRepository;
import com.jobagent.server.repository.JobPostRepository;
import com.jobagent.server.repository.MessageDraftRepository;
import com.jobagent.server.repository.MessageRepository;
import com.jobagent.server.repository.PluginTokenRepository;
import com.jobagent.server.repository.ResumeRepository;
import com.jobagent.server.repository.TaskRepository;
import com.jobagent.server.service.WorkerClient;
import com.jobagent.server.store.ConversationEntity;
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
            .thenReturn(new WorkerJobMatchResponse(80, List.of("r1"), List.of("risk1")));
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
            .thenReturn(new WorkerJobMatchResponse(60, List.of("r1"), List.of("risk1")));

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
            .andExpect(jsonPath("$.reply.intent").value("INTERVIEW"));

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
        jobPostRepository.deleteAll();
        resumeRepository.deleteAll();
        taskRepository.deleteAll();
        pluginTokenRepository.deleteAll();
    }

    private void seedTokenAndTask() {
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
            "SEMI",
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
