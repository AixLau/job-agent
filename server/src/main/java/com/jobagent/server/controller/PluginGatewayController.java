package com.jobagent.server.controller;

import com.jobagent.server.dto.ActionReportRequest;
import com.jobagent.server.dto.AnalysisResult;
import com.jobagent.server.dto.ChatReportRequest;
import com.jobagent.server.dto.ChatReportResponse;
import com.jobagent.server.dto.DraftItem;
import com.jobagent.server.dto.HeartbeatRequest;
import com.jobagent.server.dto.PageReportRequest;
import com.jobagent.server.dto.PageReportResponse;
import com.jobagent.server.dto.RecommendationItem;
import com.jobagent.server.dto.ReplyItem;
import com.jobagent.server.dto.ReplyResult;
import com.jobagent.server.dto.StatusResponse;
import com.jobagent.server.service.PluginAnalysisService;
import com.jobagent.server.store.DashboardStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/plugin")
public class PluginGatewayController {

    private final PluginAnalysisService analysisService;
    private final DashboardStore store;

    public PluginGatewayController(PluginAnalysisService analysisService, DashboardStore store) {
        this.analysisService = analysisService;
        this.store = store;
    }

    @PostMapping("/page/report")
    public PageReportResponse pageReport(@RequestBody PageReportRequest request) {
        AnalysisResult analysis = analysisService.analyzePage(request);
        DraftItem draft = analysisService.buildDraft(request);
        RecommendationItem recommendation = analysisService.toRecommendation(request, analysis);

        store.addRecommendation(recommendation);
        store.addDraft(draft);

        return new PageReportResponse("ok", analysis, draft);
    }

    @PostMapping("/chat/report")
    public ChatReportResponse chatReport(@RequestBody ChatReportRequest request) {
        ReplyResult replyResult = analysisService.analyzeChat(request);
        ReplyItem replyItem = analysisService.toReplyItem(request, replyResult);
        store.addReply(replyItem);
        return new ChatReportResponse("ok", replyResult);
    }

    @PostMapping("/action/report")
    public StatusResponse actionReport(@RequestBody ActionReportRequest request) {
        return new StatusResponse("ok");
    }

    @PostMapping("/heartbeat")
    public StatusResponse heartbeat(@RequestBody HeartbeatRequest request) {
        return new StatusResponse("ok");
    }
}
