import assert from "node:assert/strict";
import { test } from "node:test";

import { login } from "../src/lib/auth.js";

test("login posts credentials and returns response body", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (url, options) => {
    assert.equal(url, "http://example.com/api/auth/login");
    const body = JSON.parse(options.body);
    assert.equal(body.account, "alice");
    assert.equal(body.password, "pwd");
    return {
      ok: true,
      json: async () => ({ access_token: "token" }),
    };
  };
  try {
    const data = await login("http://example.com", {
      account: "alice",
      password: "pwd",
    });
    assert.equal(data.access_token, "token");
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("login throws when response is not ok", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => ({ ok: false, status: 401 });
  try {
    await assert.rejects(
      () => login("http://example.com", { account: "a", password: "b" }),
      /login failed/i
    );
  } finally {
    globalThis.fetch = originalFetch;
  }
});
