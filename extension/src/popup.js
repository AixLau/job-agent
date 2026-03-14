const pingButton = document.getElementById("ping");
const statusEl = document.getElementById("status");

const setStatus = (text) => {
  statusEl.textContent = text;
};

pingButton.addEventListener("click", async () => {
  setStatus("Pinging...");
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  if (!tab?.id) {
    setStatus("No active tab");
    return;
  }

  chrome.tabs.sendMessage(tab.id, { type: "PING" }, (response) => {
    if (chrome.runtime.lastError) {
      setStatus("No content script");
      return;
    }
    setStatus(`Response: ${response?.type ?? "unknown"}`);
  });
});
