import assert from "node:assert/strict";
import { test } from "node:test";

import { fetchDashboard, fallbackDashboard } from "../src/lib/dashboard.js";

test("fetchDashboard returns fallback when response is not ok", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => ({ ok: false });
  try {
    const data = await fetchDashboard("http://example.com", "token");
    assert.deepEqual(data, fallbackDashboard);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("fetchDashboard sends auth header and normalizes response body", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (url, options) => {
    assert.equal(url, "http://example.com/api/dashboard");
    assert.equal(options.headers.Authorization, "Bearer token");
    return {
      ok: true,
      json: async () => ({
        metrics: { recommendations: 1, drafts: 0, replies: 0, interviews: 0 },
        recommendations: [
          {
            job_post_id: "job-1",
            title: "Title",
            company: "Company",
            score: 80,
            reasons: ["匹配：产品相关"],
            risks: ["risk"],
            status: "ACTIVE",
          },
        ],
        drafts: [
          {
            draft_id: "draft-1",
            conversation_id: "conv-1",
            content: "content",
            approved: false,
            created_at: "2024-01-01T00:00:00Z",
          },
        ],
        replies: [
          {
            conversation_id: "conv-2",
            summary: "summary",
            intent: "NEEDS_REPLY",
            updated_at: "2024-01-01T00:01:00Z",
          },
        ],
        interviews: [
          {
            conversation_id: "conv-3",
            company: "Interview Co",
            title: "Role",
            scheduled_at: "2024-01-02T00:00:00Z",
          },
        ],
        updated_at: "2024-01-01T00:00:00Z",
      }),
    };
  };
  try {
    const data = await fetchDashboard("http://example.com", "token");
    assert.equal(data.metrics.recommendations, 1);
    assert.equal(data.updatedAt, "2024-01-01T00:00:00Z");
    assert.equal(data.recommendations[0].jobPostId, "job-1");
    assert.deepEqual(data.recommendations[0].reasons, ["匹配：产品相关"]);
    assert.equal(data.drafts[0].draftId, "draft-1");
    assert.equal(data.drafts[0].conversationId, "conv-1");
    assert.equal(data.drafts[0].createdAt, "2024-01-01T00:00:00Z");
    assert.equal(data.replies[0].conversationId, "conv-2");
    assert.equal(data.replies[0].updatedAt, "2024-01-01T00:01:00Z");
    assert.equal(data.interviews[0].conversationId, "conv-3");
    assert.equal(data.interviews[0].scheduledAt, "2024-01-02T00:00:00Z");
  } finally {
    globalThis.fetch = originalFetch;
  }
});
