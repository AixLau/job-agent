import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { test } from "node:test";

const PACKAGE_PATH = new URL("../package.json", import.meta.url);

test("package.json declares next app scripts and deps", async () => {
  const raw = await readFile(PACKAGE_PATH, "utf-8");
  const pkg = JSON.parse(raw);

  assert.equal(pkg.name, "job-agent-web");
  assert.equal(pkg.private, true);

  assert.equal(pkg.scripts.dev, "next dev");
  assert.equal(pkg.scripts.build, "next build");
  assert.equal(pkg.scripts.start, "next start");

  assert.ok(pkg.dependencies.next);
  assert.ok(pkg.dependencies.react);
  assert.ok(pkg.dependencies["react-dom"]);
});
