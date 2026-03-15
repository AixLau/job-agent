importScripts(chrome.runtime.getURL("src/api.js"));

const { postPageReport, postChatReport, postHeartbeat } =
  self.JobAgentApi || {};

const HEARTBEAT_INTERVAL_MS = 30000;

chrome.runtime.onInstalled.addListener(() => {
  console.info("Job Agent extension installed");
});

const sendHeartbeat = () => {
  chrome.storage.local.get(["plugin_token"], (result) => {
    const pluginToken = result?.plugin_token;
    if (!pluginToken || !postHeartbeat) {
      return;
    }
    postHeartbeat({ status: "ok", ts: Date.now() }, pluginToken).catch((error) => {
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
    if (postChatReport) {
      postChatReport(payload).catch((error) => {
        console.warn("Chat report failed", error);
        if (error?.status === 401) {
          chrome.storage.local.remove(["plugin_token"]);
        }
      });
    }
  }
});
