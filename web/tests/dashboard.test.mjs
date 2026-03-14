import assert from "node:assert/strict";
import { test } from "node:test";

import { fetchDashboard, fallbackDashboard } from "../src/lib/dashboard.js";

test("fetchDashboard returns fallback when response is not ok", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => ({ ok: false });
  try {
    const data = await fetchDashboard("http://example.com");
    assert.deepEqual(data, fallbackDashboard);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("fetchDashboard returns response body when ok", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => ({
    ok: true,
    json: async () => ({
      metrics: { recommendations: 1, drafts: 0, replies: 0, interviews: 0 },
      recommendations: [],
      drafts: [],
      replies: [],
    }),
  });
  try {
    const data = await fetchDashboard("http://example.com");
    assert.equal(data.metrics.recommendations, 1);
  } finally {
    globalThis.fetch = originalFetch;
  }
});
