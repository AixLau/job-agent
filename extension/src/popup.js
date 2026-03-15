export const initPopup = ({
  doc = document,
  browserApi = chrome,
  jobAgentApi = window.JobAgentApi,
} = {}) => {
  const statusEl = doc.getElementById("status");
  const formEl = doc.getElementById("login-form");
  const accountEl = doc.getElementById("account");
  const passwordEl = doc.getElementById("password");
  const automationToggleEl = doc.getElementById("automation-toggle");
  const automationStatusEl = doc.getElementById("automation-status");
  const openSidePanelEl = doc.getElementById("open-sidepanel");

  const setStatus = (text) => {
    if (statusEl) {
      statusEl.textContent = text;
    }
  };

  const setAutomationStatus = (paused) => {
    if (automationToggleEl) {
      automationToggleEl.checked = Boolean(paused);
    }
    if (automationStatusEl) {
      automationStatusEl.textContent = paused ? "Automation paused" : "Automation active";
    }
  };

  const refreshAutomationState = () =>
    new Promise((resolve) => {
      browserApi.storage.local.get(["automation_paused", "panel_state"], (result) => {
        setAutomationStatus(Boolean(result?.automation_paused));
        if (result?.panel_state?.status) {
          setStatus(`Current: ${result.panel_state.status}`);
        }
        resolve(result);
      });
    });

  formEl?.addEventListener("submit", async (event) => {
    event.preventDefault();
    const account = accountEl?.value?.trim() || "";
    const password = passwordEl?.value || "";
    if (!account || !password) {
      setStatus("Missing credentials");
      return;
    }
    setStatus("Logging in...");
    try {
      const token = await jobAgentApi.loginAndStoreToken(account, password);
      setStatus(token ? "Logged in" : "Login failed");
    } catch (error) {
      setStatus("Login failed");
    }
  });

  automationToggleEl?.addEventListener("change", () => {
    browserApi.storage.local.set({ automation_paused: automationToggleEl.checked }, () => {
      setAutomationStatus(automationToggleEl.checked);
      setStatus(automationToggleEl.checked ? "Automation paused" : "Automation active");
    });
  });

  openSidePanelEl?.addEventListener("click", async () => {
    browserApi.tabs.query({ active: true, currentWindow: true }, async (tabs) => {
      try {
        const tabId = tabs?.[0]?.id;
        if (browserApi.sidePanel?.open && tabId != null) {
          await browserApi.sidePanel.open({ tabId });
          setStatus("Side panel opened");
          return;
        }
        setStatus("Side panel unavailable");
      } catch (error) {
        setStatus("Open side panel failed");
      }
    });
  });

  browserApi.storage.onChanged?.addListener((changes, area) => {
    if (area === "local" && changes.automation_paused) {
      setAutomationStatus(Boolean(changes.automation_paused.newValue));
    }
  });

  return { refreshAutomationState };
};

if (typeof window !== "undefined" && typeof chrome !== "undefined") {
  window.addEventListener("DOMContentLoaded", () => {
    const popup = initPopup();
    popup.refreshAutomationState();
  });
}
