console.debug("Job Agent content script loaded");

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message && message.type === "PING") {
    sendResponse({ type: "PONG", source: "content" });
  }
});
