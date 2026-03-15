import assert from "node:assert/strict";
import { test } from "node:test";

import { createTask, fetchTasks, fallbackTasks } from "../src/lib/tasks.js";

test("fetchTasks returns fallback when response is not ok", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => ({ ok: false });
  try {
    const data = await fetchTasks("http://example.com", "token");
    assert.deepEqual(data, fallbackTasks);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("fetchTasks returns task list payload", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (url, options) => {
    assert.equal(url, "http://example.com/api/tasks");
    assert.equal(options.headers.Authorization, "Bearer token");
    return {
      ok: true,
      json: async () => ({ tasks: [{ id: "t1" }] }),
    };
  };
  try {
    const data = await fetchTasks("http://example.com", "token");
    assert.deepEqual(data, [{ id: "t1" }]);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("createTask posts payload and returns response", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (url, options) => {
    assert.equal(url, "http://example.com/api/tasks");
    assert.equal(options.method, "POST");
    assert.equal(options.headers.Authorization, "Bearer token");
    const body = JSON.parse(options.body);
    assert.equal(body.title, "Role");
    return {
      ok: true,
      json: async () => ({ task: { id: "t2" } }),
    };
  };
  try {
    const data = await createTask("http://example.com", "token", { title: "Role" });
    assert.equal(data.task.id, "t2");
  } finally {
    globalThis.fetch = originalFetch;
  }
});
