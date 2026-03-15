import assert from "node:assert/strict";
import { createRequire } from "node:module";
import { test } from "node:test";

const require = createRequire(import.meta.url);
const { buildAuthHeaders, buildAssistantState, fetchJson } = require("../src/api.js");

test("buildAuthHeaders sets X-Plugin-Token", () => {
  const headers = buildAuthHeaders("token-123");
  assert.equal(headers.get("Content-Type"), "application/json");
  assert.equal(headers.get("X-Plugin-Token"), "token-123");
});

test("buildAssistantState returns confirm view when draft exists but auto send disabled", () => {
  const state = buildAssistantState({
    reply: { intent: "FOLLOW_UP" },
    draft: { content: "你好，我补充一下项目经历。" },
    auto_send: false,
  });

  assert.equal(state.mode, "confirm");
  assert.equal(state.hasDraft, true);
  assert.equal(state.autoSend, false);
});

test("buildAssistantState returns auto view when auto send enabled", () => {
  const state = buildAssistantState({
    reply: { intent: "INTERVIEW" },
    draft: { content: "你好，我可以参加下午的面试。" },
    auto_send: true,
  });

  assert.equal(state.mode, "auto_send");
  assert.equal(state.hasDraft, true);
  assert.equal(state.intent, "INTERVIEW");
});

test("fetchJson returns null for empty successful response", async () => {
  global.fetch = async () => ({
    ok: true,
    status: 204,
    headers: new Headers(),
    text: async () => "",
  });

  await assert.doesNotReject(async () => {
    const result = await fetchJson("http://localhost/empty", {});
    assert.equal(result, null);
  });
});

test("fetchJson falls back to plain text when response is not json", async () => {
  global.fetch = async () => ({
    ok: true,
    status: 200,
    headers: new Headers({ "Content-Type": "text/plain" }),
    text: async () => "ok",
  });

  const result = await fetchJson("http://localhost/text", {});
  assert.equal(result, "ok");
});
