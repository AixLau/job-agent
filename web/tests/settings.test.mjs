import assert from "node:assert/strict";
import { test } from "node:test";

import { fallbackSettings, fetchSettings, saveSettings } from "../src/lib/settings.js";

test("fetchSettings returns normalized settings payload", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (url, options) => {
    assert.equal(url, "http://example.com/api/settings");
    assert.equal(options.headers.Authorization, "Bearer token");
    return {
      ok: true,
      json: async () => ({
        settings: {
          default_automation_level: "AUTO",
          auto_send_enabled: true,
          high_risk_requires_review: true,
          chat_immediate_auto_send: true,
          daily_action_limit: 80,
        },
      }),
    };
  };
  try {
    const data = await fetchSettings("http://example.com", "token");
    assert.equal(data.defaultAutomationLevel, "AUTO");
    assert.equal(data.chatImmediateAutoSend, true);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("saveSettings posts normalized settings payload", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (url, options) => {
    assert.equal(url, "http://example.com/api/settings");
    assert.equal(options.method, "POST");
    const body = JSON.parse(options.body);
    assert.equal(body.default_automation_level, "AUTO");
    assert.equal(body.chat_immediate_auto_send, true);
    return {
      ok: true,
      json: async () => ({ settings: body }),
    };
  };
  try {
    const data = await saveSettings("http://example.com", "token", {
      defaultAutomationLevel: "AUTO",
      autoSendEnabled: true,
      highRiskRequiresReview: true,
      chatImmediateAutoSend: true,
      dailyActionLimit: 80,
    });
    assert.equal(data.defaultAutomationLevel, "AUTO");
    assert.equal(data.dailyActionLimit, 80);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("fetchSettings falls back on error", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => ({ ok: false });
  try {
    const data = await fetchSettings("http://example.com", "token");
    assert.deepEqual(data, fallbackSettings);
  } finally {
    globalThis.fetch = originalFetch;
  }
});
