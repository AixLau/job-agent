import assert from "node:assert/strict";
import { test } from "node:test";

import { initPopup } from "../src/popup.js";
import { initSidePanel, renderSidePanelState } from "../src/sidepanel.js";

const createElement = (id, initial = {}) => {
  const listeners = new Map();
  return {
    id,
    textContent: "",
    innerHTML: "",
    value: "",
    checked: false,
    ...initial,
    addEventListener(type, handler) {
      const existing = listeners.get(type) || [];
      existing.push(handler);
      listeners.set(type, existing);
    },
    async trigger(type, event = {}) {
      const handlers = listeners.get(type) || [];
      for (const handler of handlers) {
        await handler({
          preventDefault() {},
          currentTarget: this,
          target: this,
          ...event,
        });
      }
    },
  };
};

const createDocument = (elements) => ({
  getElementById(id) {
    return elements[id] || null;
  },
});

const tick = () => new Promise((resolve) => setTimeout(resolve, 0));

test("renderSidePanelState includes task summary and pending draft", () => {
  const html = renderSidePanelState({
    taskId: "task-1",
    pageType: "chat",
    status: "WAITING_USER",
    automationPaused: false,
    company: "公司A",
    draft: "你好，我可以参加下午的面试。",
    nextAction: "确认面试时间",
  });

  assert.ok(html.includes("task-1"));
  assert.ok(html.includes("WAITING_USER"));
  assert.ok(html.includes("确认面试时间"));
  assert.ok(html.includes("公司A"));
});

test("renderSidePanelState shows paused automation state", () => {
  const html = renderSidePanelState({
    taskId: "task-2",
    pageType: "detail",
    status: "ANALYZED",
    automationPaused: true,
  });

  assert.ok(html.includes("已暂停"));
});

test("renderSidePanelState escapes html in task content", () => {
  const html = renderSidePanelState({
    taskId: "<script>alert(1)</script>",
    draft: "<img src=x onerror=alert(2)>",
    nextAction: "<b>review</b>",
  });

  assert.ok(!html.includes("<script>"));
  assert.ok(!html.includes("<img"));
  assert.ok(html.includes("&lt;script&gt;alert(1)&lt;/script&gt;"));
  assert.ok(html.includes("&lt;b&gt;review&lt;/b&gt;"));
});

test("initSidePanel reads storage and refreshes when panel state changes", async () => {
  const root = createElement("sidepanel-root");
  const document = createDocument({
    "sidepanel-root": root,
  });

  const listeners = [];
  let storageState = {
    panel_state: {
      taskId: "task-3",
      pageType: "detail",
      status: "ANALYZED",
      company: "Company A",
      draft: "Draft A",
      nextAction: "Wait",
    },
    automation_paused: false,
  };

  const browserApi = {
    storage: {
      local: {
        get(_keys, callback) {
          callback(storageState);
        },
      },
      onChanged: {
        addListener(listener) {
          listeners.push(listener);
        },
        removeListener() {},
      },
    },
  };

  const controller = initSidePanel({ doc: document, browserApi });
  await controller.refresh();
  assert.ok(root.innerHTML.includes("task-3"));
  assert.ok(root.innerHTML.includes("运行中"));

  storageState = {
    panel_state: {
      ...storageState.panel_state,
      status: "WAITING_USER",
      nextAction: "Confirm interview",
    },
    automation_paused: true,
  };

  listeners[0](
    {
      panel_state: { newValue: storageState.panel_state },
      automation_paused: { newValue: true },
    },
    "local"
  );
  await tick();

  assert.ok(root.innerHTML.includes("WAITING_USER"));
  assert.ok(root.innerHTML.includes("Confirm interview"));
  assert.ok(root.innerHTML.includes("已暂停"));
});

test("initPopup reflects automation state and opens the side panel", async () => {
  const elements = {
    status: createElement("status"),
    "login-form": createElement("login-form"),
    account: createElement("account"),
    password: createElement("password"),
    "automation-toggle": createElement("automation-toggle"),
    "automation-status": createElement("automation-status"),
    "open-sidepanel": createElement("open-sidepanel"),
  };
  const document = createDocument(elements);

  const storageWrites = [];
  const opened = [];
  const storageListeners = [];
  const browserApi = {
    storage: {
      local: {
        get(_keys, callback) {
          callback({ automation_paused: true });
        },
        set(payload, callback) {
          storageWrites.push(payload);
          callback?.();
        },
      },
      onChanged: {
        addListener(listener) {
          storageListeners.push(listener);
        },
        removeListener() {},
      },
    },
    tabs: {
      query(_queryInfo, callback) {
        callback([{ id: 77 }]);
      },
    },
    sidePanel: {
      async open(options) {
        opened.push(options);
      },
    },
  };

  const popup = initPopup({
    doc: document,
    browserApi,
    jobAgentApi: {
      async loginAndStoreToken() {
        return "token";
      },
    },
  });

  await popup.refreshAutomationState();
  assert.equal(elements["automation-toggle"].checked, true);
  assert.equal(elements["automation-status"].textContent, "Automation paused");

  await elements["open-sidepanel"].trigger("click");
  assert.deepEqual(opened, [{ tabId: 77 }]);
  assert.equal(elements.status.textContent, "Side panel opened");

  storageListeners[0](
    {
      automation_paused: { newValue: false },
    },
    "local"
  );
  await tick();

  assert.equal(elements["automation-toggle"].checked, false);
  assert.equal(elements["automation-status"].textContent, "Automation active");

  await elements["automation-toggle"].trigger("change");
  assert.deepEqual(storageWrites.at(-1), { automation_paused: false });
});

test("initPopup reports side panel open failure", async () => {
  const elements = {
    status: createElement("status"),
    "login-form": createElement("login-form"),
    account: createElement("account"),
    password: createElement("password"),
    "automation-toggle": createElement("automation-toggle"),
    "automation-status": createElement("automation-status"),
    "open-sidepanel": createElement("open-sidepanel"),
  };
  const document = createDocument(elements);

  const browserApi = {
    storage: {
      local: {
        get(_keys, callback) {
          callback({ automation_paused: false });
        },
        set(_payload, callback) {
          callback?.();
        },
      },
      onChanged: {
        addListener() {},
        removeListener() {},
      },
    },
    tabs: {
      query(_queryInfo, callback) {
        callback([{ id: 88 }]);
      },
    },
    sidePanel: {
      async open() {
        throw new Error("denied");
      },
    },
  };

  const popup = initPopup({
    doc: document,
    browserApi,
    jobAgentApi: {
      async loginAndStoreToken() {
        return "token";
      },
    },
  });

  await popup.refreshAutomationState();
  await elements["open-sidepanel"].trigger("click");
  await tick();

  assert.equal(elements.status.textContent, "Open side panel failed");
});
