import assert from "node:assert/strict";
import { test } from "node:test";

import { fetchFollows, fallbackFollows } from "../src/lib/follows.js";

test("fetchFollows returns items and sends auth header", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (url, options) => {
    assert.equal(url, "http://example.com/api/follows?page=0&size=10");
    assert.equal(options.headers.Authorization, "Bearer token");
    return {
      ok: true,
      json: async () => ({
        items: [
          {
            job_post_id: "job-1",
            title: "Product Manager",
            company: "Company A",
            created_at: "2024-01-01T00:00:00Z",
          },
        ],
        page: 0,
        size: 10,
        total: 1,
      }),
    };
  };
  try {
    const data = await fetchFollows("http://example.com", "token", 0, 10);
    assert.equal(data.items.length, 1);
    assert.equal(data.items[0].jobPostId, "job-1");
    assert.equal(data.page, 0);
    assert.equal(data.size, 10);
    assert.equal(data.total, 1);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("fetchFollows returns fallback on 401", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => ({ ok: false, status: 401 });
  try {
    const data = await fetchFollows("http://example.com", "token", 0, 10);
    assert.deepEqual(data, fallbackFollows);
  } finally {
    globalThis.fetch = originalFetch;
  }
});
