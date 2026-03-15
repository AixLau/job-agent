importScripts(chrome.runtime.getURL("src/api.js"));

const {
  postPageReport,
  postChatReport,
  postActionReport,
  postHeartbeat,
  buildHeartbeatPayload,
} =
  self.JobAgentApi || {};

const HEARTBEAT_INTERVAL_MS = 30000;

chrome.runtime.onInstalled.addListener(() => {
  console.info("Job Agent extension installed");
});

const sendHeartbeat = () => {
  chrome.storage.local.get(["plugin_token", "user_id", "task_id", "last_tab_id", "automation_paused"], (result) => {
    const pluginToken = result?.plugin_token;
    if (!pluginToken || !postHeartbeat || !buildHeartbeatPayload) {
      return;
    }
    const payload = buildHeartbeatPayload({
      user_id: result?.user_id,
      task_id: result?.task_id,
      tab_id: result?.last_tab_id ? String(result.last_tab_id) : "background",
      status: result?.automation_paused ? "paused" : "active",
      ts: Date.now(),
    });
    postHeartbeat(payload, pluginToken).catch((error) => {
      console.warn("Heartbeat failed", error);
      if (error?.status === 401) {
        chrome.storage.local.remove(["plugin_token"]);
      }
    });
  });
};

setInterval(sendHeartbeat, HEARTBEAT_INTERVAL_MS);

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message && message.type === "PING") {
    sendResponse({ type: "PONG", source: "background" });
  }
  if (message && message.type === "PAGE_REPORT") {
    const { type, ...payload } = message;
    chrome.storage.local.set({
      task_id: payload.task_id || "demo-task",
      last_tab_id: sender?.tab?.id || "background",
    });
    if (postPageReport) {
      postPageReport(payload).catch((error) => {
        console.warn("Page report failed", error);
        if (error?.status === 401) {
          chrome.storage.local.remove(["plugin_token"]);
        }
      });
    }
  }
  if (message && message.type === "CHAT_REPORT") {
    const { type, ...payload } = message;
    chrome.storage.local.set({
      task_id: payload.task_id || "demo-task",
      last_tab_id: sender?.tab?.id || "background",
    });
    if (postChatReport) {
      postChatReport(payload)
        .then((response) => {
          if (sender?.tab?.id && response?.auto_send) {
            chrome.tabs.sendMessage(sender.tab.id, {
              type: "AUTO_SEND",
              payload: response,
            });
          }
        })
        .catch((error) => {
          console.warn("Chat report failed", error);
          if (error?.status === 401) {
            chrome.storage.local.remove(["plugin_token"]);
          }
        });
    }
  }
  if (message && message.type === "ACTION_REPORT") {
    if (postActionReport) {
      postActionReport(message.payload).catch((error) => {
        console.warn("Action report failed", error);
        if (error?.status === 401) {
          chrome.storage.local.remove(["plugin_token"]);
        }
      });
    }
  }
});
