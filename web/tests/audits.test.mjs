import assert from "node:assert/strict";
import { test } from "node:test";

import { fallbackAudits, fetchAudits } from "../src/lib/audits.js";

test("fetchAudits returns paginated data and sends auth header", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (url, options) => {
    assert.equal(url, "http://example.com/api/audits?page=0&size=10");
    assert.equal(options.headers.Authorization, "Bearer token");
    return {
      ok: true,
      json: async () => ({
        items: [
          {
            action_type: "JOB_FOLLOW",
            created_at: "2024-01-01T00:00:00Z",
            result: "OK",
            payload: "{\"job_post_id\":\"job-1\"}",
            model_output: "{\"status\":\"followed\"}",
            risk_tags: ["高优先级"],
          },
        ],
        page: 0,
        size: 10,
        total: 1,
      }),
    };
  };
  try {
    const data = await fetchAudits("http://example.com", "token", 0, 10);
    assert.equal(data.items.length, 1);
    assert.equal(data.items[0].actionType, "JOB_FOLLOW");
    assert.equal(data.items[0].result, "OK");
    assert.equal(data.items[0].modelOutput, "{\"status\":\"followed\"}");
    assert.deepEqual(data.items[0].riskTags, ["高优先级"]);
    assert.equal(data.page, 0);
    assert.equal(data.size, 10);
    assert.equal(data.total, 1);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("fetchAudits returns fallback on 401", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => ({ ok: false, status: 401 });
  try {
    const data = await fetchAudits("http://example.com", "token", 0, 10);
    assert.deepEqual(data, fallbackAudits);
  } finally {
    globalThis.fetch = originalFetch;
  }
});
