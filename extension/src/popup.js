const statusEl = document.getElementById("status");
const formEl = document.getElementById("login-form");
const accountEl = document.getElementById("account");
const passwordEl = document.getElementById("password");
const automationToggleEl = document.getElementById("automation-toggle");

const setStatus = (text) => {
  statusEl.textContent = text;
};

formEl.addEventListener("submit", async (event) => {
  event.preventDefault();
  const account = accountEl.value.trim();
  const password = passwordEl.value;
  if (!account || !password) {
    setStatus("Missing credentials");
    return;
  }
  setStatus("Logging in...");
  try {
    const token = await window.JobAgentApi.loginAndStoreToken(account, password);
    setStatus(token ? "Logged in" : "Login failed");
  } catch (error) {
    setStatus("Login failed");
  }
});

chrome.storage.local.get(["automation_paused"], (result) => {
  automationToggleEl.checked = Boolean(result?.automation_paused);
});

automationToggleEl.addEventListener("change", () => {
  chrome.storage.local.set({ automation_paused: automationToggleEl.checked });
  setStatus(automationToggleEl.checked ? "Automation paused" : "Automation active");
});
