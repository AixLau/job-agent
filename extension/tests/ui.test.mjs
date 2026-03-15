import assert from "node:assert/strict";
import { test } from "node:test";

import { buildOverlayHtml } from "../src/ui.js";

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
