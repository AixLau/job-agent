import assert from "node:assert/strict";
import { test } from "node:test";

import { fetchTasks, fallbackTasks } from "../src/lib/tasks.js";

test("fetchTasks returns fallback when response is not ok", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => ({ ok: false });
  try {
    const data = await fetchTasks("http://example.com");
    assert.deepEqual(data, fallbackTasks);
  } finally {
    globalThis.fetch = originalFetch;
  }
});
