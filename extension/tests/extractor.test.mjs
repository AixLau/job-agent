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

test("extract list cards with salary/exp/city", () => {
  const cards = [
    {
      querySelector: (selector) => {
        if (selector === "h2") {
          return { textContent: "Product Manager" };
        }
        if (selector.includes("company")) {
          return { textContent: "Company B" };
        }
        if (selector.includes("salary")) {
          return { textContent: "20-30k" };
        }
        if (selector.includes("experience")) {
          return { textContent: "3-5年" };
        }
        if (selector.includes("city")) {
          return { textContent: "Shanghai" };
        }
        if (selector === "a") {
          return { href: "https://example.com/job_detail/456" };
        }
        return null;
      },
      getAttribute: (name) => (name === "data-job-id" ? "456" : null),
    },
  ];
  const documentStub = {
    title: "List Page",
    querySelector: () => null,
    querySelectorAll: (selector) => {
      if (selector.includes("job-card")) {
        return cards;
      }
      return [];
    },
  };
  const locationStub = { href: "https://example.com/jobs" };

  const payload = withGlobals({ document: documentStub, location: locationStub }, () =>
    extractJobPayload()
  );

  assert.equal(payload.cards.length, 1);
  assert.deepEqual(payload.cards[0], {
    title: "Product Manager",
    company: "Company B",
    salary: "20-30k",
    experience: "3-5年",
    city: "Shanghai",
    external_id: "456",
    url: "https://example.com/job_detail/456",
  });
});

test("extractChatMessages returns messages for chat pages", () => {
  const nodes = [
    {
      textContent: "hello",
      className: "message from-hr",
      dataset: { role: "hr" },
      getAttribute: (name) => {
        if (name === "data-role") {
          return "hr";
        }
        return null;
      },
    },
    {
      textContent: "world",
      className: "message from-user",
      dataset: { role: "user" },
      getAttribute: (name) => {
        if (name === "data-ts") {
          return "2024-01-01T00:00:00Z";
        }
        if (name === "data-role") {
          return "user";
        }
        return null;
      },
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
  assert.equal(messages[1].role, "user");
  assert.equal(messages[1].text, "world");
  assert.ok(messages[0].id);
});
