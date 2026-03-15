if (typeof importScripts === "function" && typeof chrome !== "undefined") {
  importScripts(chrome.runtime.getURL("src/api.js"));
}

const {
  buildAssistantState,
  postPageReport,
  postChatReport,
  postActionReport,
  postHeartbeat,
  buildHeartbeatPayload,
} = globalThis.JobAgentApi || {};

const HEARTBEAT_ALARM_NAME = "job-agent-heartbeat";
const HEARTBEAT_PERIOD_MINUTES = 0.5;

const scheduleHeartbeatAlarm = (browserApi = chrome) => {
  if (!browserApi?.alarms?.create) {
    return;
  }
  browserApi.alarms.create(HEARTBEAT_ALARM_NAME, {
    periodInMinutes: HEARTBEAT_PERIOD_MINUTES,
  });
};

const shouldHandleHeartbeatAlarm = (alarm) => alarm?.name === HEARTBEAT_ALARM_NAME;

const registerDefaultPanelState = (browserApi = chrome) => {
  browserApi?.storage?.local?.set?.({
    panel_state: {
      taskId: "demo-task",
      pageType: "idle",
      status: "IDLE",
      automationPaused: false,
      company: "",
      draft: "",
      nextAction: "",
    },
  });
};

const enableSidePanelAction = (browserApi = chrome) => {
  if (browserApi?.sidePanel?.setPanelBehavior) {
    browserApi.sidePanel.setPanelBehavior({ openPanelOnActionClick: true }).catch(() => {});
  }
};

const updatePanelState = (patch = {}, browserApi = chrome) =>
  new Promise((resolve) => {
    browserApi.storage.local.get(["panel_state", "automation_paused"], (result) => {
      const nextState = {
        ...(result?.panel_state || {}),
        ...patch,
        automationPaused: patch.automationPaused ?? Boolean(result?.automation_paused),
      };
      browserApi.storage.local.set({ panel_state: nextState }, () => resolve(nextState));
    });
  });

const sendHeartbeat = (browserApi = chrome) => {
  browserApi.storage.local.get(["plugin_token", "user_id", "task_id", "last_tab_id", "automation_paused"], (result) => {
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
        browserApi.storage.local.remove(["plugin_token"]);
      }
    });
  });
};

const registerBackground = (browserApi = chrome) => {
  browserApi.runtime.onInstalled.addListener(() => {
    console.info("Job Agent extension installed");
    registerDefaultPanelState(browserApi);
    enableSidePanelAction(browserApi);
    scheduleHeartbeatAlarm(browserApi);
  });

  browserApi.runtime.onStartup?.addListener?.(() => {
    scheduleHeartbeatAlarm(browserApi);
  });

  browserApi.alarms?.onAlarm?.addListener?.((alarm) => {
    if (shouldHandleHeartbeatAlarm(alarm)) {
      sendHeartbeat(browserApi);
    }
  });

  browserApi.runtime.onMessage.addListener((message, sender, sendResponse) => {
    if (message && message.type === "PING") {
      sendResponse({ type: "PONG", source: "background" });
    }
    if (message && message.type === "GET_PANEL_STATE") {
      browserApi.storage.local.get(["panel_state", "automation_paused"], (result) => {
        sendResponse({
          ...(result?.panel_state || {}),
          automationPaused: Boolean(result?.automation_paused),
        });
      });
      return true;
    }
    if (message && message.type === "PANEL_STATE_UPDATE") {
      updatePanelState(message.payload || {}, browserApi).then((state) => sendResponse({ status: "ok", state }));
      return true;
    }
    if (message && message.type === "SET_AUTOMATION_PAUSED") {
      browserApi.storage.local.set({ automation_paused: Boolean(message.value) }, () => {
        updatePanelState({ automationPaused: Boolean(message.value) }, browserApi).then((state) => {
          sendResponse({ status: "ok", state });
        });
      });
      return true;
    }
    if (message && message.type === "PAGE_REPORT") {
      const { type, ...payload } = message;
      browserApi.storage.local.set({
        task_id: payload.task_id || "demo-task",
        last_tab_id: sender?.tab?.id || "background",
      });
      if (postPageReport) {
        postPageReport(payload)
          .then((response) => {
            updatePanelState({
              taskId: payload.task_id || "demo-task",
              pageType: payload.page_type || "detail",
              status: "ANALYZED",
              company: payload?.extracted_json?.company || "",
              draft: response?.draft?.content || "",
              nextAction: response?.analysis?.reasons?.[0] || "",
            }, browserApi);
            if (sender?.tab?.id) {
              browserApi.tabs.sendMessage(sender.tab.id, {
                type: "SHOW_OVERLAY",
                payload: response,
              });
            }
          })
          .catch((error) => {
            console.warn("Page report failed", error);
            if (error?.status === 401) {
              browserApi.storage.local.remove(["plugin_token"]);
            }
          });
      }
    }
    if (message && message.type === "CHAT_REPORT") {
      const { type, ...payload } = message;
      browserApi.storage.local.set({
        task_id: payload.task_id || "demo-task",
        last_tab_id: sender?.tab?.id || "background",
      });
      if (postChatReport) {
        postChatReport(payload)
          .then((response) => {
            const assistantState = buildAssistantState ? buildAssistantState(response) : {};
            updatePanelState({
              taskId: payload.task_id || "demo-task",
              pageType: "chat",
              status: response?.reply?.intent || "CHAT_ACTIVE",
              draft: response?.draft?.content || "",
              nextAction: response?.reply?.next_action || response?.reply?.nextAction || "",
              intent: assistantState.intent || "",
              mode: assistantState.mode || "idle",
            }, browserApi);
            if (sender?.tab?.id && response?.draft) {
              browserApi.tabs.sendMessage(sender.tab.id, {
                type: "SHOW_OVERLAY",
                payload: response,
              });
            }
            if (sender?.tab?.id && response?.auto_send) {
              browserApi.tabs.sendMessage(sender.tab.id, {
                type: "AUTO_SEND",
                payload: response,
              });
            }
          })
          .catch((error) => {
            console.warn("Chat report failed", error);
            if (error?.status === 401) {
              browserApi.storage.local.remove(["plugin_token"]);
            }
          });
      }
    }
    if (message && message.type === "ACTION_REPORT") {
      if (postActionReport) {
        updatePanelState({
          lastAction: message.payload?.action_type || "",
        }, browserApi);
        postActionReport(message.payload).catch((error) => {
          console.warn("Action report failed", error);
          if (error?.status === 401) {
            browserApi.storage.local.remove(["plugin_token"]);
          }
        });
      }
    }
    return false;
  });
};

if (typeof chrome !== "undefined" && chrome?.runtime?.onMessage) {
  registerBackground(chrome);
}

const backgroundApi = {
  HEARTBEAT_ALARM_NAME,
  HEARTBEAT_PERIOD_MINUTES,
  scheduleHeartbeatAlarm,
  shouldHandleHeartbeatAlarm,
  sendHeartbeat,
  registerBackground,
};

if (typeof module !== "undefined" && module.exports) {
  module.exports = backgroundApi;
}
