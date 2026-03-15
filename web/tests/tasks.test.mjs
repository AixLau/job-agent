import assert from "node:assert/strict";
import { test } from "node:test";

import {
  createTask,
  deriveTaskFormFromStrategy,
  fallbackTasks,
  fetchTasks,
  normalizeTaskPayload,
  parseTaskListInput,
} from "../src/lib/tasks.js";

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
    assert.deepEqual(body.exclude, ["外包", "派遣"]);
    assert.deepEqual(body.preferences, ["B端"]);
    assert.equal(body.automation_level, "AUTO");
    return {
      ok: true,
      json: async () => ({ task: { id: "t2" } }),
    };
  };
  try {
    const data = await createTask("http://example.com", "token", {
      title: "Role",
      exclude: "外包, 派遣",
      preferences: "B端",
      automationLevel: "AUTO",
    });
    assert.equal(data.task.id, "t2");
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("parseTaskListInput splits common separators", () => {
  assert.deepEqual(parseTaskListInput("外包，派遣 / 996"), ["外包", "派遣", "996"]);
});

test("normalizeTaskPayload maps camel fields and arrays for task api", () => {
  assert.deepEqual(
    normalizeTaskPayload({
      title: "产品经理",
      city: "上海",
      salary: "20k-30k",
      experience: "3-5年",
      exclude: "外包,派遣",
      preferences: ["B端", "增长"],
      automationLevel: "AUTO",
      strategyText: "上海 产品经理",
    }),
    {
      title: "产品经理",
      city: "上海",
      salary: "20k-30k",
      experience: "3-5年",
      exclude: ["外包", "派遣"],
      preferences: ["B端", "增长"],
      automation_level: "AUTO",
      strategy_text: "上海 产品经理",
    },
  );
});

test("deriveTaskFormFromStrategy extracts structured task fields from strategy", () => {
  assert.deepEqual(
    deriveTaskFormFromStrategy({
      city: "上海",
      title: "产品经理",
      salary: "20k-30k",
      experience: "3-5年",
      automationLevel: "AUTO",
      exclude: ["外包"],
      preferences: ["B端"],
    }),
    {
      city: "上海",
      title: "产品经理",
      salary: "20k-30k",
      experience: "3-5年",
      automationLevel: "AUTO",
      exclude: "外包",
      preferences: "B端",
    },
  );
});
