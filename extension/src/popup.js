const analyzeButton = document.getElementById("analyze");
const statusEl = document.getElementById("status");
const resultEl = document.getElementById("result");

const API_BASE = "http://localhost:8080";

const setStatus = (text) => {
  statusEl.textContent = text;
};

const setResult = (text) => {
  resultEl.textContent = text;
};

analyzeButton.addEventListener("click", async () => {
  setStatus("Analyzing...");
  setResult("");
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  if (!tab?.id) {
    setStatus("No active tab");
    return;
  }

  chrome.tabs.sendMessage(tab.id, { type: "EXTRACT_PAGE" }, async (payload) => {
    if (chrome.runtime.lastError) {
      setStatus("No content script");
      return;
    }
    try {
      const response = await fetch(`${API_BASE}/plugin/page/report`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          task_id: "demo-task",
          ...payload,
        }),
      });
      if (!response.ok) {
        setStatus("Server error");
        return;
      }
      const data = await response.json();
      setStatus("Done");
      setResult(`Score: ${data.analysis?.score ?? "-"}`);
    } catch (error) {
      setStatus("Fetch failed");
    }
  });
});
