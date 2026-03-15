import assert from "node:assert/strict";
import { test } from "node:test";

import { fallbackRecommendations, fetchRecommendations } from "../src/lib/recommendations.js";

test("fetchRecommendations returns normalized recommendation list", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (url, options) => {
    assert.equal(url, "http://example.com/api/recommendations");
    assert.equal(options.headers.Authorization, "Bearer token");
    return {
      ok: true,
      json: async () => ({
        items: [
          {
            job_post_id: "job-1",
            title: "产品经理",
            company: "公司A",
            score: 88,
            reasons: ["匹配：产品相关"],
            risks: ["外包"],
            status: "SHORTLISTED",
          },
        ],
      }),
    };
  };
  try {
    const data = await fetchRecommendations("http://example.com", "token");
    assert.equal(data.length, 1);
    assert.equal(data[0].jobPostId, "job-1");
    assert.deepEqual(data[0].reasons, ["匹配：产品相关"]);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("fetchRecommendations falls back on error", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => ({ ok: false });
  try {
    const data = await fetchRecommendations("http://example.com", "token");
    assert.deepEqual(data, fallbackRecommendations);
  } finally {
    globalThis.fetch = originalFetch;
  }
});
