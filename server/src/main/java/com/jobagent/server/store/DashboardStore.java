package com.jobagent.server.store;

import com.jobagent.server.dto.DashboardMetrics;
import com.jobagent.server.dto.DashboardResponse;
import com.jobagent.server.dto.DraftItem;
import com.jobagent.server.dto.RecommendationItem;
import com.jobagent.server.dto.ReplyItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class DashboardStore {

    private static final int MAX_ITEMS = 20;

    private final Deque<RecommendationItem> recommendations = new ConcurrentLinkedDeque<>();
    private final Deque<DraftItem> drafts = new ConcurrentLinkedDeque<>();
    private final Deque<ReplyItem> replies = new ConcurrentLinkedDeque<>();

    public void addRecommendation(RecommendationItem item) {
        addWithLimit(recommendations, item);
    }

    public void addDraft(DraftItem item) {
        addWithLimit(drafts, item);
    }

    public void addReply(ReplyItem item) {
        addWithLimit(replies, item);
    }

    public DashboardResponse snapshot() {
        List<RecommendationItem> recList = new ArrayList<>(recommendations);
        List<DraftItem> draftList = new ArrayList<>(drafts);
        List<ReplyItem> replyList = new ArrayList<>(replies);

        int interviews = (int) replyList.stream()
            .filter(item -> "INTERVIEW".equalsIgnoreCase(item.intent()))
            .count();

        DashboardMetrics metrics = new DashboardMetrics(
            recList.size(),
            draftList.size(),
            replyList.size(),
            interviews
        );

        return new DashboardResponse(metrics, recList, draftList, replyList);
    }

    public void clear() {
        recommendations.clear();
        drafts.clear();
        replies.clear();
    }

    private <T> void addWithLimit(Deque<T> deque, T item) {
        deque.addFirst(item);
        while (deque.size() > MAX_ITEMS) {
            deque.removeLast();
        }
    }
}
