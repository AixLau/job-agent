import assert from "node:assert/strict";
import { test } from "node:test";

import { blacklistCompany, followJob, ignoreJob } from "../src/lib/jobActions.js";

test("followJob posts to follow endpoint with auth", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (url, options) => {
    assert.equal(url, "http://example.com/api/jobs/job-1/follow");
    assert.equal(options.method, "POST");
    assert.equal(options.headers.Authorization, "Bearer token");
    return { ok: true, json: async () => ({ status: "ok" }) };
  };
  try {
    const data = await followJob("http://example.com", "token", "job-1");
    assert.equal(data.status, "ok");
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("ignoreJob posts to ignore endpoint with auth", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (url, options) => {
    assert.equal(url, "http://example.com/api/jobs/job-2/ignore");
    assert.equal(options.method, "POST");
    assert.equal(options.headers.Authorization, "Bearer token");
    return { ok: true, json: async () => ({ status: "ok" }) };
  };
  try {
    const data = await ignoreJob("http://example.com", "token", "job-2");
    assert.equal(data.status, "ok");
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("blacklistCompany posts to blacklist endpoint with auth", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (url, options) => {
    assert.equal(url, "http://example.com/api/blacklist/company");
    assert.equal(options.method, "POST");
    assert.equal(options.headers.Authorization, "Bearer token");
    const body = JSON.parse(options.body);
    assert.equal(body.company_name, "Company A");
    assert.equal(body.source, "boss");
    return { ok: true, json: async () => ({ status: "ok" }) };
  };
  try {
    const data = await blacklistCompany("http://example.com", "token", {
      companyName: "Company A",
      source: "boss",
    });
    assert.equal(data.status, "ok");
  } finally {
    globalThis.fetch = originalFetch;
  }
});
