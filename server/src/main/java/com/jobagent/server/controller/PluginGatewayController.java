package com.jobagent.server.controller;

import com.jobagent.server.dto.ActionReportRequest;
import com.jobagent.server.dto.ChatReportRequest;
import com.jobagent.server.dto.HeartbeatRequest;
import com.jobagent.server.dto.PageReportRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/plugin")
public class PluginGatewayController {

    private static final Map<String, String> OK_RESPONSE = Map.of("status", "ok");

    @PostMapping("/page/report")
    public Map<String, String> pageReport(@RequestBody PageReportRequest request) {
        return OK_RESPONSE;
    }

    @PostMapping("/chat/report")
    public Map<String, String> chatReport(@RequestBody ChatReportRequest request) {
        return OK_RESPONSE;
    }

    @PostMapping("/action/report")
    public Map<String, String> actionReport(@RequestBody ActionReportRequest request) {
        return OK_RESPONSE;
    }

    @PostMapping("/heartbeat")
    public Map<String, String> heartbeat(@RequestBody HeartbeatRequest request) {
        return OK_RESPONSE;
    }
}
