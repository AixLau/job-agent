import assert from "node:assert/strict";
import { test } from "node:test";

import { detectPageType, extractChatMessages, extractJobPayload, hashText } from "../src/extractor.js";

const withGlobals = (overrides, fn) => {
  const previous = {
    document: global.document,
    location: global.location,
  };
  global.document = overrides.document ?? global.document;
  global.location = overrides.location ?? global.location;
  try {
    return fn();
  } finally {
    global.document = previous.document;
    global.location = previous.location;
  }
};

test("detectPageType returns detail for detail urls", () => {
  const result = detectPageType("https://example.com/job_detail/123");
  assert.equal(result, "detail");
});

test("hashText is deterministic", () => {
  const first = hashText("abc");
  const second = hashText("abc");
  assert.equal(first, second);
  assert.ok(first.length > 0);
});

test("extractJobPayload returns source/external_id/title/company", () => {
  const documentStub = {
    title: "Fallback Title",
    querySelector: (selector) => {
      if (selector === "h1") {
        return { textContent: "Job Title" };
      }
      if (selector.includes("company")) {
        return { textContent: "Company A" };
      }
      return null;
    },
  };
  const locationStub = { href: "https://example.com/job_detail/987" };

  const payload = withGlobals({ document: documentStub, location: locationStub }, () =>
    extractJobPayload()
  );

  assert.equal(payload.source, "zhipin");
  assert.equal(payload.external_id, "987");
  assert.equal(payload.title, "Job Title");
  assert.equal(payload.company, "Company A");
});

test("extractChatMessages returns messages for chat pages", () => {
  const nodes = [
    {
      textContent: "hello",
      getAttribute: () => null,
    },
    {
      textContent: "world",
      getAttribute: (name) => (name === "data-ts" ? "2024-01-01T00:00:00Z" : null),
    },
  ];
  const documentStub = {
    querySelectorAll: () => nodes,
  };
  const locationStub = { href: "https://example.com/chat/1" };

  const messages = withGlobals({ document: documentStub, location: locationStub }, () =>
    extractChatMessages()
  );

  assert.equal(messages.length, 2);
  assert.equal(messages[0].role, "hr");
  assert.equal(messages[0].text, "hello");
  assert.equal(messages[1].text, "world");
  assert.ok(messages[0].id);
});
