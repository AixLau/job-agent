const escapeHtml = (value) =>
  String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");

export const renderSidePanelState = (state = {}) => `
  <section class="sidepanel-card">
    <div class="sidepanel-row">
      <span class="sidepanel-label">任务</span>
      <strong>${escapeHtml(state.taskId || "未绑定任务")}</strong>
    </div>
    <div class="sidepanel-row">
      <span class="sidepanel-label">页面</span>
      <strong>${escapeHtml(state.pageType || "idle")}</strong>
    </div>
    <div class="sidepanel-row">
      <span class="sidepanel-label">状态</span>
      <strong>${escapeHtml(state.status || "IDLE")}</strong>
    </div>
    <div class="sidepanel-row">
      <span class="sidepanel-label">自动化</span>
      <strong>${state.automationPaused ? "已暂停" : "运行中"}</strong>
    </div>
    <div class="sidepanel-row">
      <span class="sidepanel-label">公司</span>
      <strong>${escapeHtml(state.company || "-")}</strong>
    </div>
    <div class="sidepanel-block">
      <span class="sidepanel-label">下一步</span>
      <p>${escapeHtml(state.nextAction || "等待新建议")}</p>
    </div>
    <div class="sidepanel-block">
      <span class="sidepanel-label">草稿</span>
      <p>${escapeHtml(state.draft || "暂无草稿")}</p>
    </div>
  </section>
`;

export const initSidePanel = ({
  doc = document,
  browserApi = chrome,
} = {}) => {
  const root = doc.getElementById("sidepanel-root");
  const pauseButton = doc.getElementById("pause-automation");
  const resumeButton = doc.getElementById("resume-automation");

  const readPanelState = () =>
    new Promise((resolve) => {
      browserApi.storage.local.get(["panel_state", "automation_paused"], (result) => {
        resolve({
          ...(result?.panel_state || {}),
          automationPaused: Boolean(result?.automation_paused ?? result?.panel_state?.automationPaused),
        });
      });
    });

  const refresh = async () => {
    if (!root) {
      return;
    }
    const state = await readPanelState();
    root.innerHTML = renderSidePanelState(state);
  };

  pauseButton?.addEventListener("click", () => {
    browserApi.runtime.sendMessage?.({ type: "SET_AUTOMATION_PAUSED", value: true }, () => refresh());
  });
  resumeButton?.addEventListener("click", () => {
    browserApi.runtime.sendMessage?.({ type: "SET_AUTOMATION_PAUSED", value: false }, () => refresh());
  });

  browserApi.storage.onChanged?.addListener((changes, area) => {
    if (area === "local" && (changes.panel_state || changes.automation_paused)) {
      refresh();
    }
  });

  return { refresh };
};

if (typeof window !== "undefined") {
  window.addEventListener("DOMContentLoaded", () => {
    const controller = initSidePanel();
    controller.refresh();
  });
}
