import assert from "node:assert/strict";
import { test } from "node:test";

import { fallbackInterviews, fetchInterviews } from "../src/lib/interviews.js";

test("fetchInterviews returns normalized interview list", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (url, options) => {
    assert.equal(url, "http://example.com/api/interviews");
    assert.equal(options.headers.Authorization, "Bearer token");
    return {
      ok: true,
      json: async () => ({
        items: [
          {
            conversation_id: "conv-1",
            company: "公司A",
            title: "产品经理",
            draft_id: "draft-1",
            draft_content: "可以参加明天下午的面试，感谢安排。",
            next_action: "确认面试时间",
            scheduled_at: "2024-01-02T00:00:00Z",
          },
        ],
      }),
    };
  };
  try {
    const data = await fetchInterviews("http://example.com", "token");
    assert.equal(data.length, 1);
    assert.equal(data[0].conversationId, "conv-1");
    assert.equal(data[0].scheduledAt, "2024-01-02T00:00:00Z");
    assert.equal(data[0].draftContent, "可以参加明天下午的面试，感谢安排。");
    assert.equal(data[0].nextAction, "确认面试时间");
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("fetchInterviews falls back on error", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => ({ ok: false });
  try {
    const data = await fetchInterviews("http://example.com", "token");
    assert.deepEqual(data, fallbackInterviews);
  } finally {
    globalThis.fetch = originalFetch;
  }
});
