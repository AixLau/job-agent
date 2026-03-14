chrome.runtime.onInstalled.addListener(() => {
  console.info("Job Agent extension installed");
});

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message && message.type === "PING") {
    sendResponse({ type: "PONG", source: "background" });
  }
});
