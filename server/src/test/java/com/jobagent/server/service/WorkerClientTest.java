package com.jobagent.server.service;

import com.jobagent.server.dto.GoalParseRequest;
import com.jobagent.server.dto.GoalParseResponse;
import com.jobagent.server.dto.WorkerJobMatchRequest;
import com.jobagent.server.dto.WorkerDraftRequest;
import com.jobagent.server.dto.WorkerDraftResponse;
import com.jobagent.server.dto.WorkerJobMatchResponse;
import com.jobagent.server.dto.WorkerReplyClassifyRequest;
import com.jobagent.server.dto.WorkerReplyClassifyResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(WorkerClient.class)
@TestPropertySource(properties = {
    "job-agent.worker.base-url=http://worker.test",
    "job-agent.worker.token=secret"
})
class WorkerClientTest {

    @Autowired
    private WorkerClient workerClient;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void parse_goal_sends_token_and_idempotency_key() {
        server.expect(ExpectedCount.once(), requestTo("http://worker.test/worker/goal-parse"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("X-Worker-Token", "secret"))
            .andExpect(jsonPath("$.idempotency_key").value("key-1"))
            .andRespond(withSuccess("{\"strategy_json\":\"{\\\"goal\\\":\\\"x\\\"}\"}", MediaType.APPLICATION_JSON));

        GoalParseResponse response = workerClient.parseGoal(new GoalParseRequest(
            "task-1",
            "GOAL_PARSE",
            "x",
            "key-1"
        ));

        assertThat(response.strategyJson()).isEqualTo("{\"goal\":\"x\"}");
    }

    @Test
    void job_match_sends_token_and_idempotency_key() {
        server.expect(ExpectedCount.once(), requestTo("http://worker.test/worker/job-match"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("X-Worker-Token", "secret"))
            .andExpect(jsonPath("$.idempotency_key").value("key-2"))
            .andRespond(withSuccess("{\"score\":80,\"reasons\":[\"r\"],\"risks\":[\"low\"]}", MediaType.APPLICATION_JSON));

        WorkerJobMatchResponse response = workerClient.jobMatch(new WorkerJobMatchRequest(
            "task-1",
            "JOB_MATCH",
            Map.of("id", "jp-1"),
            Map.of("id", "r-1"),
            Map.of("goal", "x"),
            "key-2"
        ));

        assertThat(response.score()).isEqualTo(80);
        assertThat(response.reasons()).isEqualTo(List.of("r"));
        assertThat(response.risks()).isEqualTo(List.of("low"));
    }

    @Test
    void draft_sends_token_and_idempotency_key() {
        server.expect(ExpectedCount.once(), requestTo("http://worker.test/worker/draft"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("X-Worker-Token", "secret"))
            .andExpect(jsonPath("$.idempotency_key").value("key-3"))
            .andRespond(withSuccess("{\"content\":\"hello\"}", MediaType.APPLICATION_JSON));

        WorkerDraftResponse response = workerClient.buildDraft(new WorkerDraftRequest(
            "task-1",
            "DRAFT",
            Map.of("id", "c-1"),
            Map.of("id", "jp-1"),
            Map.of("id", "r-1"),
            "key-3"
        ));

        assertThat(response.content()).isEqualTo("hello");
    }

    @Test
    void reply_classify_sends_token_and_idempotency_key() {
        server.expect(ExpectedCount.once(), requestTo("http://worker.test/worker/reply-classify"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("X-Worker-Token", "secret"))
            .andExpect(jsonPath("$.idempotency_key").value("key-4"))
            .andRespond(withSuccess("{\"intent\":\"INTERVIEW\",\"summary\":\"ok\",\"next_action\":\"REPLY\"}", MediaType.APPLICATION_JSON));

        WorkerReplyClassifyResponse response = workerClient.replyClassify(new WorkerReplyClassifyRequest(
            "task-1",
            "REPLY_CLASSIFY",
            Map.of("id", "c-1"),
            List.of(Map.of("id", "m-1")),
            "m-1",
            "key-4"
        ));

        assertThat(response.intent()).isEqualTo("INTERVIEW");
        assertThat(response.summary()).isEqualTo("ok");
        assertThat(response.nextAction()).isEqualTo("REPLY");
    }

    @Test
    void job_match_retries_on_failure() {
        server.expect(ExpectedCount.once(), requestTo("http://worker.test/worker/job-match"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withServerError());

        server.expect(ExpectedCount.once(), requestTo("http://worker.test/worker/job-match"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("{\"score\":90,\"reasons\":[],\"risks\":[]}", MediaType.APPLICATION_JSON));

        WorkerJobMatchResponse response = workerClient.jobMatch(new WorkerJobMatchRequest(
            "task-1",
            "JOB_MATCH",
            Map.of("id", "jp-1"),
            Map.of("id", "r-1"),
            Map.of("goal", "x"),
            "retry-key"
        ));

        assertThat(response.score()).isEqualTo(90);
    }

    @Test
    void parse_goal_throws_on_empty_body() {
        server.expect(ExpectedCount.times(3), requestTo("http://worker.test/worker/goal-parse"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> workerClient.parseGoal(new GoalParseRequest(
            "task-1",
            "GOAL_PARSE",
            "x",
            "key-empty"
        ))).isInstanceOf(Exception.class);
    }
}
