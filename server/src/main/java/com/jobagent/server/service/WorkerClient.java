package com.jobagent.server.service;

import com.jobagent.server.dto.GoalParseRequest;
import com.jobagent.server.dto.GoalParseResponse;
import com.jobagent.server.dto.WorkerDraftRequest;
import com.jobagent.server.dto.WorkerDraftResponse;
import com.jobagent.server.dto.WorkerFollowUpRequest;
import com.jobagent.server.dto.WorkerFollowUpResponse;
import com.jobagent.server.dto.WorkerJobMatchRequest;
import com.jobagent.server.dto.WorkerJobMatchResponse;
import com.jobagent.server.dto.WorkerReplyClassifyRequest;
import com.jobagent.server.dto.WorkerReplyClassifyResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Service
public class WorkerClient {

    private static final int MAX_ATTEMPTS = 3;

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String token;

    public WorkerClient(RestTemplateBuilder builder,
                        @Value("${job-agent.worker.base-url}") String baseUrl,
                        @Value("${job-agent.worker.token}") String token) {
        this.restTemplate = builder
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(10))
            .build();
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.token = token;
    }

    public GoalParseResponse parseGoal(GoalParseRequest request) {
        return post("/worker/goal-parse", request, GoalParseResponse.class);
    }

    public WorkerJobMatchResponse jobMatch(WorkerJobMatchRequest request) {
        return post("/worker/job-match", request, WorkerJobMatchResponse.class);
    }

    public WorkerDraftResponse buildDraft(WorkerDraftRequest request) {
        return post("/worker/draft", request, WorkerDraftResponse.class);
    }

    public WorkerReplyClassifyResponse replyClassify(WorkerReplyClassifyRequest request) {
        return post("/worker/reply-classify", request, WorkerReplyClassifyResponse.class);
    }

    public WorkerFollowUpResponse followUp(WorkerFollowUpRequest request) {
        return post("/worker/follow-up", request, WorkerFollowUpResponse.class);
    }

    private <T> T post(String path, Object request, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null && !token.isBlank()) {
            headers.add("X-Worker-Token", token);
        }
        HttpEntity<Object> entity = new HttpEntity<>(request, headers);
        RestClientException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                ResponseEntity<T> response = restTemplate.postForEntity(baseUrl + path, entity, responseType);
                T body = response.getBody();
                if (body == null) {
                    throw new RestClientException("worker response body is empty");
                }
                return body;
            } catch (RestClientException ex) {
                last = ex;
            }
        }
        throw last == null ? new RestClientException("worker request failed") : last;
    }

    private String normalizeBaseUrl(String url) {
        if (url == null) {
            return "";
        }
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
