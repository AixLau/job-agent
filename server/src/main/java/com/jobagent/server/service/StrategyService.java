package com.jobagent.server.service;

import com.jobagent.server.dto.GoalParseRequest;
import org.springframework.stereotype.Service;

@Service
public class StrategyService {

    private final WorkerClient workerClient;

    public StrategyService(WorkerClient workerClient) {
        this.workerClient = workerClient;
    }

    public String parse(String strategyText, String taskId) {
        String idempotencyKey = IdempotencyKeys.goalParse(taskId, strategyText);
        GoalParseRequest request = new GoalParseRequest(
            taskId,
            "GOAL_PARSE",
            strategyText,
            idempotencyKey
        );
        return workerClient.parseGoal(request).strategyJson();
    }
}
