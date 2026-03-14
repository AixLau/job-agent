import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { test } from "node:test";

const MANIFEST_PATH = new URL("../manifest.json", import.meta.url);

test("manifest has required MV3 fields", async () => {
  const raw = await readFile(MANIFEST_PATH, "utf-8");
  const manifest = JSON.parse(raw);

  assert.equal(manifest.manifest_version, 3);
  assert.equal(typeof manifest.name, "string");
  assert.ok(manifest.name.length > 0);
  assert.equal(typeof manifest.version, "string");
  assert.ok(manifest.version.length > 0);

  assert.deepEqual(manifest.background, {
    service_worker: "src/background.js",
  });

  assert.ok(Array.isArray(manifest.permissions));
  assert.ok(manifest.permissions.includes("storage"));
  assert.ok(manifest.permissions.includes("activeTab"));

  assert.ok(Array.isArray(manifest.host_permissions));
  assert.ok(manifest.host_permissions.includes("https://*.zhipin.com/*"));

  assert.deepEqual(manifest.action, {
    default_title: "Job Agent",
    default_popup: "src/popup.html",
  });

  assert.ok(Array.isArray(manifest.content_scripts));
  assert.ok(manifest.content_scripts.length > 0);

  const contentScript = manifest.content_scripts[0];
  assert.ok(contentScript.matches.includes("https://*.zhipin.com/*"));
  assert.deepEqual(contentScript.js, ["src/content.js"]);
});
