import assert from "node:assert/strict";
import { createRequire } from "node:module";
import { test } from "node:test";

import { buildOverlayHtml, performAutoSend } from "../src/ui.js";

const require = createRequire(import.meta.url);
const { buildHeartbeatPayload } = require("../src/api.js");

test("buildOverlayHtml includes score", () => {
  const html = buildOverlayHtml({
    score: 80,
    reasons: ["reason"],
    risks: ["risk"],
    draft: { content: "hello" },
  });
  assert.ok(html.includes("80"));
});

test("buildOverlayHtml escapes html content", () => {
  const html = buildOverlayHtml({
    score: "<img onerror=alert(1)>",
    reasons: ["<script>alert(1)</script>"],
    risks: ["<img src=x onerror=alert(2)>"],
    draft: { content: "<b>bold</b>" },
  });
  assert.ok(!html.includes("<script>"));
  assert.ok(!html.includes("<img"));
  assert.ok(html.includes("&lt;script&gt;"));
  assert.ok(html.includes("&lt;img"));
  assert.ok(html.includes("&lt;b&gt;bold&lt;/b&gt;"));
});

test("buildHeartbeatPayload includes required fields", () => {
  const payload = buildHeartbeatPayload({
    user_id: "user-1",
    task_id: "task-1",
    tab_id: "tab-9",
    status: "active",
    ts: 12345,
  });
  assert.equal(payload.user_id, "user-1");
  assert.equal(payload.task_id, "task-1");
  assert.equal(payload.tab_id, "tab-9");
  assert.equal(payload.status, "active");
  assert.equal(payload.ts, 12345);
});

test("auto send reports SEND and DELIVERED", async () => {
  const reports = [];
  const inputEl = {
    value: "",
    dispatchEvent: () => {},
  };
  const sendButtonEl = {
    clicked: false,
    click() {
      this.clicked = true;
    },
  };

  const result = await performAutoSend({
    draftText: "hello",
    inputEl,
    sendButtonEl,
    reportAction: async (payload) => {
      reports.push(payload);
    },
    taskId: "task-1",
  });

  assert.equal(result.status, "ok");
  assert.equal(inputEl.value, "hello");
  assert.equal(sendButtonEl.clicked, true);
  assert.deepEqual(
    reports.map((item) => item.action_type),
    ["SEND", "DELIVERED"]
  );
});

test("auto send reports FAILED when cannot send", async () => {
  const reports = [];
  const result = await performAutoSend({
    draftText: "hello",
    inputEl: null,
    sendButtonEl: null,
    reportAction: async (payload) => {
      reports.push(payload);
    },
    taskId: "task-1",
  });

  assert.equal(result.status, "failed");
  assert.deepEqual(
    reports.map((item) => item.action_type),
    ["FAILED"]
  );
});
