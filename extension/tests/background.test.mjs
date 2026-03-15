import assert from "node:assert/strict";
import { createRequire } from "node:module";
import { test } from "node:test";

const require = createRequire(import.meta.url);
const {
  HEARTBEAT_ALARM_NAME,
  HEARTBEAT_PERIOD_MINUTES,
  scheduleHeartbeatAlarm,
  shouldHandleHeartbeatAlarm,
} = require("../src/background.js");

test("scheduleHeartbeatAlarm registers recurring mv3 alarm", () => {
  const calls = [];
  scheduleHeartbeatAlarm({
    alarms: {
      create(name, options) {
        calls.push({ name, options });
      },
    },
  });

  assert.deepEqual(calls, [
    {
      name: HEARTBEAT_ALARM_NAME,
      options: {
        periodInMinutes: HEARTBEAT_PERIOD_MINUTES,
      },
    },
  ]);
});

test("shouldHandleHeartbeatAlarm only accepts heartbeat alarm name", () => {
  assert.equal(shouldHandleHeartbeatAlarm({ name: HEARTBEAT_ALARM_NAME }), true);
  assert.equal(shouldHandleHeartbeatAlarm({ name: "other-alarm" }), false);
  assert.equal(shouldHandleHeartbeatAlarm(null), false);
});
