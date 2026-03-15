const API_BASE = "http://localhost:8080";

const buildAuthHeaders = (pluginToken) => {
  const headers = new Headers();
  headers.set("Content-Type", "application/json");
  if (pluginToken) {
    headers.set("X-Plugin-Token", pluginToken);
  }
  return headers;
};

const getBrowserId = () =>
  new Promise((resolve) => {
    if (!chrome?.storage?.local) {
      const fallback = globalThis.crypto?.randomUUID
        ? globalThis.crypto.randomUUID()
        : `${Date.now()}-${Math.random()}`;
      resolve(fallback);
      return;
    }
    chrome.storage.local.get(["browser_id"], (result) => {
      if (result?.browser_id) {
        resolve(result.browser_id);
        return;
      }
      const generated = globalThis.crypto?.randomUUID
        ? globalThis.crypto.randomUUID()
        : `${Date.now()}-${Math.random()}`;
      chrome.storage.local.set({ browser_id: generated }, () => {
        resolve(generated);
      });
    });
  });

const readPluginToken = () =>
  new Promise((resolve) => {
    if (!chrome?.storage?.local) {
      resolve("");
      return;
    }
    chrome.storage.local.get(["plugin_token"], (result) => {
      resolve(result?.plugin_token || "");
    });
  });

const fetchJson = async (url, options) => {
  const response = await fetch(url, options);
  if (!response.ok) {
    const error = new Error(`Request failed: ${response.status}`);
    error.status = response.status;
    throw error;
  }
  const rawText = typeof response.text === "function" ? await response.text() : "";
  if (!rawText) {
    return null;
  }
  try {
    return JSON.parse(rawText);
  } catch (_error) {
    return rawText;
  }
};

const loginAndStoreToken = async (account, password) => {
  const loginPayload = { account, password };
  const loginResponse = await fetchJson(`${API_BASE}/api/auth/login`, {
    method: "POST",
    headers: buildAuthHeaders(),
    body: JSON.stringify(loginPayload),
  });
  const accessToken = loginResponse.access_token;
  if (!accessToken) {
    throw new Error("Missing access token");
  }

  const pluginPayload = {
    access_token: accessToken,
    browser_id: await getBrowserId(),
  };
  const pluginResponse = await fetchJson(`${API_BASE}/api/auth/plugin/token`, {
    method: "POST",
    headers: buildAuthHeaders(),
    body: JSON.stringify(pluginPayload),
  });
  const pluginToken = pluginResponse.plugin_token;
  if (!pluginToken) {
    throw new Error("Missing plugin token");
  }
  if (chrome?.storage?.local) {
    chrome.storage.local.set({ plugin_token: pluginToken, user_id: account });
  }
  return pluginToken;
};

const postPageReport = async (payload) => {
  const pluginToken = await readPluginToken();
  return fetchJson(`${API_BASE}/plugin/page/report`, {
    method: "POST",
    headers: buildAuthHeaders(pluginToken),
    body: JSON.stringify(payload),
  });
};

const postChatReport = async (payload) => {
  const pluginToken = await readPluginToken();
  return fetchJson(`${API_BASE}/plugin/chat/report`, {
    method: "POST",
    headers: buildAuthHeaders(pluginToken),
    body: JSON.stringify(payload),
  });
};

const postActionReport = async (payload) => {
  const pluginToken = await readPluginToken();
  return fetchJson(`${API_BASE}/plugin/action/report`, {
    method: "POST",
    headers: buildAuthHeaders(pluginToken),
    body: JSON.stringify(payload),
  });
};

const buildHeartbeatPayload = ({ user_id, task_id, tab_id, status, ts }) => ({
  user_id: user_id || "unknown",
  task_id: task_id || "demo-task",
  tab_id: tab_id || "background",
  status: status || "active",
  ts: ts || Date.now(),
});

const buildAssistantState = (response = {}) => {
  const hasDraft = Boolean(response?.draft?.content || response?.draft);
  const autoSend = Boolean(response?.auto_send || response?.autoSend);
  return {
    mode: autoSend ? "auto_send" : hasDraft ? "confirm" : "idle",
    hasDraft,
    autoSend,
    intent: response?.reply?.intent || "",
    requiresReview: Boolean(response?.requires_review || response?.requiresReview),
  };
};

const postHeartbeat = async (payload, pluginToken) => {
  if (!pluginToken) {
    return null;
  }
  return fetchJson(`${API_BASE}/plugin/heartbeat`, {
    method: "POST",
    headers: buildAuthHeaders(pluginToken),
    body: JSON.stringify(payload),
  });
};

const api = {
  buildAuthHeaders,
  fetchJson,
  getBrowserId,
  loginAndStoreToken,
  postPageReport,
  postChatReport,
  postActionReport,
  buildHeartbeatPayload,
  buildAssistantState,
  postHeartbeat,
};

if (typeof window !== "undefined") {
  window.JobAgentApi = api;
}

if (typeof self !== "undefined") {
  self.JobAgentApi = api;
}

if (typeof module !== "undefined" && module.exports) {
  module.exports = api;
}
