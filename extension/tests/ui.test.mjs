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

test("buildOverlayHtml exposes confirm, send and pause controls", () => {
  const html = buildOverlayHtml({
    score: 92,
    reasons: ["匹配度高"],
    risks: ["高风险"],
    reply: { intent: "INTERVIEW", next_action: "确认面试时间" },
    draft: { content: "你好，我可以参加下午的面试。" },
    auto_send: false,
    requiresReview: true,
  });

  assert.ok(html.includes('data-action="fill"'));
  assert.ok(html.includes('data-action="send"'));
  assert.ok(html.includes('data-action="pause"'));
  assert.ok(html.includes("高风险需人工确认"));
});

test("buildOverlayHtml accepts snake_case automation fields", () => {
  const html = buildOverlayHtml({
    score: 88,
    reasons: ["适合自动发送"],
    risks: [],
    draft: { content: "你好，我想进一步了解岗位情况。" },
    auto_send: true,
    requires_review: true,
  });

  assert.ok(html.includes("高风险需人工确认"));
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

test("auto send returns review_required when draft must be confirmed manually", async () => {
  const reports = [];
  const result = await performAutoSend({
    draftText: "需要人工确认",
    inputEl: { value: "", dispatchEvent: () => {} },
    sendButtonEl: { click() {} },
    requiresReview: true,
    reportAction: async (payload) => {
      reports.push(payload);
    },
    taskId: "task-2",
  });

  assert.equal(result.status, "review_required");
  assert.deepEqual(
    reports.map((item) => item.action_type),
    ["REVIEW_REQUIRED"]
  );
});
