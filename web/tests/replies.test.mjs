import assert from "node:assert/strict";
import { test } from "node:test";

import { fallbackReplies, fetchReplies } from "../src/lib/replies.js";

test("fetchReplies returns normalized reply list", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (url, options) => {
    assert.equal(url, "http://example.com/api/replies");
    assert.equal(options.headers.Authorization, "Bearer token");
    return {
      ok: true,
      json: async () => ({
        items: [
          {
            conversation_id: "conv-1",
            job_post_id: "job-1",
            company: "公司A",
            summary: "需要跟进",
            intent: "FOLLOW_UP",
            next_action: "补充所需材料",
            priority: "HIGH",
            follow_up_at: "2024-01-01T01:00:00Z",
            updated_at: "2024-01-01T00:00:00Z",
          },
        ],
      }),
    };
  };
  try {
    const data = await fetchReplies("http://example.com", "token");
    assert.equal(data.length, 1);
    assert.equal(data[0].conversationId, "conv-1");
    assert.equal(data[0].jobPostId, "job-1");
    assert.equal(data[0].nextAction, "补充所需材料");
    assert.equal(data[0].priority, "HIGH");
    assert.equal(data[0].followUpAt, "2024-01-01T01:00:00Z");
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("fetchReplies falls back on error", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => ({ ok: false });
  try {
    const data = await fetchReplies("http://example.com", "token");
    assert.deepEqual(data, fallbackReplies);
  } finally {
    globalThis.fetch = originalFetch;
  }
});
