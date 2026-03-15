import assert from "node:assert/strict";
import { test } from "node:test";

import { fallbackInterviews, fetchInterviews } from "../src/lib/interviews.js";

test("fetchInterviews returns normalized interview list", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => ({
    ok: true,
    json: async () => ({
      metrics: {},
      interviews: [
        {
          conversation_id: "conv-1",
          company: "公司A",
          title: "产品经理",
          scheduled_at: "2024-01-02T00:00:00Z",
        },
      ],
    }),
  });
  try {
    const data = await fetchInterviews("http://example.com", "token");
    assert.equal(data.length, 1);
    assert.equal(data[0].conversationId, "conv-1");
    assert.equal(data[0].scheduledAt, "2024-01-02T00:00:00Z");
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
