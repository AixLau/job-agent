package com.jobagent.server.service;

import com.jobagent.server.dto.WorkerFollowUpRequest;
import com.jobagent.server.dto.WorkerFollowUpResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class FollowUpPolicyService {

    private final WorkerClient workerClient;
    private final ModelOutputValidator validator;

    public FollowUpPolicyService(WorkerClient workerClient, ModelOutputValidator validator) {
        this.workerClient = workerClient;
        this.validator = validator;
    }

    public WorkerFollowUpResponse plan(String taskId,
                                       Map<String, Object> conversation,
                                       List<Map<String, Object>> messages,
                                       String lastMessageId,
                                       String idempotencyKey) {
        WorkerFollowUpRequest request = new WorkerFollowUpRequest(
            taskId,
            "FOLLOW_UP",
            conversation,
            messages,
            lastMessageId,
            idempotencyKey
        );
        try {
            WorkerFollowUpResponse response = workerClient.followUp(request);
            if (response != null) {
                validator.validateFollowUp(response);
            }
            return response;
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "WORKER_TIMEOUT");
        }
    }
}
