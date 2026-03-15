package com.jobagent.server.service;

import com.jobagent.server.dto.AnalysisResult;
import com.jobagent.server.dto.ChatReportRequest;
import com.jobagent.server.dto.DraftItem;
import com.jobagent.server.dto.PageReportRequest;
import com.jobagent.server.dto.RecommendationItem;
import com.jobagent.server.dto.ReplyItem;
import com.jobagent.server.dto.ReplyResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class PluginAnalysisService {

    public AnalysisResult analyzePage(PageReportRequest request) {
        String text = safeText(request.rawText());
        List<String> reasons = new ArrayList<>();
        List<String> risks = new ArrayList<>();

        int score = 60;
        if (text.contains("产品")) {
            score += 15;
            reasons.add("岗位匹配：产品相关");
        }
        if (text.contains("资深") || text.contains("高级")) {
            score += 10;
            reasons.add("匹配资深要求");
        }
        if (text.contains("外包")) {
            score -= 20;
            risks.add("outsourcing");
        }
        if (text.contains("加班")) {
            score -= 10;
            risks.add("overtime");
        }

        if (reasons.isEmpty()) {
            reasons.add("信息不足，建议进一步确认");
        }

        score = Math.max(0, Math.min(100, score));
        return new AnalysisResult(score, reasons, risks);
    }

    public DraftItem buildDraft(PageReportRequest request) {
        Map<String, Object> data = request.extractedJson() == null
            ? Collections.emptyMap()
            : request.extractedJson();
        String title = firstNonBlank(
            valueOf(data, "title"),
            valueOf(data, "job_title"),
            valueOf(data, "position"),
            "该岗位"
        );
        String company = firstNonBlank(
            valueOf(data, "company"),
            valueOf(data, "company_name"),
            "贵司"
        );

        String content = String.format(
            "你好，我对%s的%s岗位很感兴趣，期待进一步沟通。",
            company,
            title
        );

        return new DraftItem(
            java.util.UUID.randomUUID().toString(),
            "",
            content,
            java.time.Instant.now(),
            false
        );
    }

    public ReplyResult analyzeChat(ChatReportRequest request) {
        String text = extractLastMessageText(request);
        if (text.contains("面试")) {
            return new ReplyResult("INTERVIEW", "HR 提到面试安排", "确认面试时间");
        }
        if (text.contains("简历") || text.contains("补充")) {
            return new ReplyResult("NEEDS_INFO", "HR 需要补充材料", "补充简历/材料");
        }
        if (text.contains("不合适") || text.contains("感谢")) {
            return new ReplyResult("REJECTED", "HR 表示暂不合适", "归档");
        }
        return new ReplyResult("FOLLOW_UP", "HR 回复待跟进", "继续跟进");
    }

    public RecommendationItem toRecommendation(PageReportRequest request, AnalysisResult analysis) {
        Map<String, Object> data = request.extractedJson() == null
            ? Collections.emptyMap()
            : request.extractedJson();
        String title = firstNonBlank(
            valueOf(data, "title"),
            valueOf(data, "job_title"),
            valueOf(data, "position"),
            "未命名岗位"
        );
        String company = firstNonBlank(
            valueOf(data, "company"),
            valueOf(data, "company_name"),
            "未知公司"
        );
        return new RecommendationItem(
            "",
            title,
            company,
            analysis.score(),
            analysis.reasons(),
            analysis.riskTags(),
            ""
        );
    }

    public ReplyItem toReplyItem(ChatReportRequest request, ReplyResult replyResult) {
        return new ReplyItem(
            request.conversationId(),
            replyResult.summary(),
            replyResult.intent(),
            java.time.Instant.now()
        );
    }

    private String extractLastMessageText(ChatReportRequest request) {
        if (request.messages() == null || request.messages().isEmpty()) {
            return "";
        }
        Map<String, Object> last = request.messages().get(request.messages().size() - 1);
        String text = valueOf(last, "text");
        if (isBlank(text)) {
            text = valueOf(last, "content");
        }
        if (isBlank(text)) {
            text = valueOf(last, "message");
        }
        return safeText(text);
    }

    private String safeText(String text) {
        return text == null ? "" : text;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String valueOf(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }
}
