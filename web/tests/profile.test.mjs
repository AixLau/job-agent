import assert from "node:assert/strict";
import { test } from "node:test";

import { fallbackProfile, fetchProfile, saveProfile } from "../src/lib/profile.js";

test("fetchProfile returns fallback when response is not ok", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => ({ ok: false });
  try {
    const data = await fetchProfile("http://example.com", "token");
    assert.deepEqual(data, fallbackProfile);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("fetchProfile returns normalized profile payload", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (url, options) => {
    assert.equal(url, "http://example.com/api/profile");
    assert.equal(options.headers.Authorization, "Bearer token");
    return {
      ok: true,
      json: async () => ({
        profile: {
          account: "alice",
          full_name: "Alice Zhang",
          phone: "13800138000",
          skills: ["PRD"],
        },
      }),
    };
  };
  try {
    const data = await fetchProfile("http://example.com", "token");
    assert.equal(data.account, "alice");
    assert.equal(data.fullName, "Alice Zhang");
    assert.equal(data.phone, "13800138000");
    assert.deepEqual(data.skills, ["PRD"]);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("saveProfile posts payload and returns normalized profile", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (url, options) => {
    assert.equal(url, "http://example.com/api/profile");
    assert.equal(options.method, "POST");
    assert.equal(options.headers.Authorization, "Bearer token");
    const body = JSON.parse(options.body);
    assert.equal(body.full_name, "Alice Zhang");
    assert.deepEqual(body.skills, ["PRD", "Growth"]);
    return {
      ok: true,
      json: async () => ({
        profile: {
          account: "alice",
          full_name: "Alice Zhang",
          skills: ["PRD", "Growth"],
        },
      }),
    };
  };
  try {
    const data = await saveProfile("http://example.com", "token", {
      fullName: "Alice Zhang",
      skills: ["PRD", "Growth"],
    });
    assert.equal(data.fullName, "Alice Zhang");
    assert.deepEqual(data.skills, ["PRD", "Growth"]);
  } finally {
    globalThis.fetch = originalFetch;
  }
});
