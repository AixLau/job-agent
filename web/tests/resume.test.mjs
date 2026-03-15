import assert from "node:assert/strict";
import { test } from "node:test";

import { confirmResume, parseResumeUpload, uploadResume } from "../src/lib/resume.js";

test("uploadResume posts text resume payload", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (url, options) => {
    assert.equal(url, "http://example.com/api/resume");
    assert.equal(options.method, "POST");
    const body = JSON.parse(options.body);
    assert.equal(body.format, "TEXT");
    return {
      ok: true,
      json: async () => ({ resume: { id: "r1" } }),
    };
  };
  try {
    const data = await uploadResume("http://example.com", "token", {
      content: "resume text",
      format: "TEXT",
    });
    assert.equal(data.resume.id, "r1");
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("parseResumeUpload posts file metadata and returns parsed preview", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (url, options) => {
    assert.equal(url, "http://example.com/api/resume/parse");
    const body = JSON.parse(options.body);
    assert.equal(body.file_name, "resume.pdf");
    assert.equal(body.format, "PDF");
    return {
      ok: true,
      json: async () => ({ parsed_json: { file_name: "resume.pdf", format: "PDF" } }),
    };
  };
  try {
    const data = await parseResumeUpload("http://example.com", "token", {
      content: "resume text",
      fileName: "resume.pdf",
      format: "PDF",
      source: "upload",
    });
    assert.equal(data.parsed_json.file_name, "resume.pdf");
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("confirmResume posts parsed payload and returns saved resume", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (url, options) => {
    assert.equal(url, "http://example.com/api/resume/confirm");
    const body = JSON.parse(options.body);
    assert.equal(body.parsed_json.file_name, "resume.pdf");
    return {
      ok: true,
      json: async () => ({ resume: { id: "r2" } }),
    };
  };
  try {
    const data = await confirmResume("http://example.com", "token", {
      content: "resume text",
      format: "PDF",
      source: "upload",
      parsedJson: { file_name: "resume.pdf" },
    });
    assert.equal(data.resume.id, "r2");
  } finally {
    globalThis.fetch = originalFetch;
  }
});
