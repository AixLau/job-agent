import assert from "node:assert/strict";
import { createRequire } from "node:module";
import { test } from "node:test";

const require = createRequire(import.meta.url);
const { buildAuthHeaders } = require("../src/api.js");

test("buildAuthHeaders sets X-Plugin-Token", () => {
  const headers = buildAuthHeaders("token-123");
  assert.equal(headers.get("Content-Type"), "application/json");
  assert.equal(headers.get("X-Plugin-Token"), "token-123");
});
