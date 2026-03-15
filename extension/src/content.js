console.debug("Job Agent content script loaded");

const MAX_TEXT_LENGTH = 5000;

const extractor = window.JobAgentExtractor || {};
const {
  detectPageType,
  extractJobPayload,
  extractChatMessages,
  hashText,
} = extractor;
const { buildOverlayHtml, performAutoSend } = window.JobAgentUI || {};

const extractText = () => {
  const raw = document.body?.innerText ?? "";
  return raw.length > MAX_TEXT_LENGTH ? raw.slice(0, MAX_TEXT_LENGTH) : raw;
};

const extractPageInfo = () => {
  const rawText = extractText();
  const jobPayload = extractJobPayload ? extractJobPayload() : {};
  const title = jobPayload.title || document.title || "未命名岗位";
  const company = jobPayload.company || "未知公司";
  const source = jobPayload.source || "";
  const externalId = jobPayload.external_id || "";
  const pageType = detectPageType ? detectPageType(location.href) : "list";
  return {
    page_type: pageType,
    raw_text: rawText,
    extracted_json: {
      title,
      company,
      source,
      external_id: externalId,
    },
    source_url: location.href,
    dom_hash: hashText ? hashText(rawText + title + company) : "",
    want_draft: pageType === "detail",
  };
};

const getTaskId = () =>
  new Promise((resolve) => {
    chrome.storage.local.get(["task_id"], (result) => {
      resolve(result?.task_id || "demo-task");
    });
  });

const isAutomationPaused = () =>
  new Promise((resolve) => {
    chrome.storage.local.get(["automation_paused"], (result) => {
      resolve(Boolean(result?.automation_paused));
    });
  });

const buildChatReport = (taskId) => {
  const messages = extractChatMessages ? extractChatMessages() : [];
  const lastMessageId = messages.length ? messages[messages.length - 1].id : "";
  const conversationId = hashText ? hashText(location.href) : location.href;
  return {
    type: "CHAT_REPORT",
    task_id: taskId,
    conversation_id: conversationId,
    messages,
    last_message_id: lastMessageId,
  };
};

const buildPageReport = async () => {
  const taskId = await getTaskId();
  const payload = extractPageInfo();
  return {
    type: "PAGE_REPORT",
    task_id: taskId,
    ...payload,
  };
};

const sendReport = async () => {
  if (!detectPageType) {
    return;
  }
  const taskId = await getTaskId();
  const pageType = detectPageType(location.href);
  if (pageType === "chat") {
    chrome.runtime.sendMessage(buildChatReport(taskId));
    return;
  }
  const payload = await buildPageReport();
  chrome.runtime.sendMessage(payload);
};

sendReport();

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message?.type === "PING") {
    sendResponse({ type: "PONG", source: "content" });
    return true;
  }
  if (message?.type === "EXTRACT_PAGE") {
    sendResponse(extractPageInfo());
    return true;
  }
  if (message?.type === "SHOW_OVERLAY") {
    renderOverlay(message.payload || {});
    sendResponse({ status: "ok" });
    return true;
  }
  if (message?.type === "AUTO_SEND") {
    handleAutoSend(message.payload || {})
      .then(() => sendResponse({ status: "ok" }))
      .catch(() => sendResponse({ status: "failed" }));
    return true;
  }
  return false;
});

const renderOverlay = (payload) => {
  if (!buildOverlayHtml) {
    return;
  }
  const existing = document.getElementById("job-agent-overlay");
  if (existing) {
    existing.remove();
  }
  const html = buildOverlayHtml(payload);
  document.body.insertAdjacentHTML("beforeend", html);
  const overlay = document.getElementById("job-agent-overlay");
  if (!overlay) {
    return;
  }
  overlay.addEventListener("click", (event) => {
    const action = event.target?.dataset?.action;
    if (action === "ignore") {
      overlay.remove();
      return;
    }
    if (action === "fill") {
      const draftText =
        overlay.querySelector("#job-agent-draft")?.value || "";
      fillDraftIntoPage(draftText);
      overlay.remove();
    }
  });
};

const fillDraftIntoPage = (text) => {
  const textarea = document.querySelector("textarea");
  if (textarea) {
    textarea.value = text;
    textarea.dispatchEvent(new Event("input", { bubbles: true }));
    return;
  }
  const editable = document.querySelector("[contenteditable='true']");
  if (editable) {
    editable.textContent = text;
    editable.dispatchEvent(new Event("input", { bubbles: true }));
  }
};

const findDraftInput = () =>
  document.querySelector("textarea") ||
  document.querySelector("[contenteditable='true']");

const findSendButton = () =>
  document.querySelector("button[type='submit']") ||
  document.querySelector("[data-action='send']") ||
  document.querySelector("button[class*='send']");

const reportAction = (payload) =>
  new Promise((resolve) => {
    chrome.runtime.sendMessage({ type: "ACTION_REPORT", payload }, () => resolve());
  });

const showAutoSendHint = (text) => {
  const existing = document.getElementById("job-agent-auto-send-hint");
  if (existing) {
    existing.remove();
  }
  const hint = document.createElement("div");
  hint.id = "job-agent-auto-send-hint";
  hint.textContent = text;
  hint.style.cssText = "position:fixed;bottom:16px;right:16px;z-index:99999;background:#111;color:#fff;padding:8px 12px;border-radius:6px;font-size:12px;";
  document.body.appendChild(hint);
  setTimeout(() => hint.remove(), 3000);
};

const handleAutoSend = async (payload) => {
  if (!performAutoSend || (await isAutomationPaused())) {
    return;
  }
  const taskId = await getTaskId();
  const result = await performAutoSend({
    draftText: payload?.draft?.content || payload?.draft || "",
    inputEl: findDraftInput(),
    sendButtonEl: findSendButton(),
    reportAction,
    taskId,
    afterSend: () => new Promise((resolve) => setTimeout(resolve, 300)),
  });
  if (result.status !== "ok") {
    showAutoSendHint("自动发送失败，请手动确认");
  }
};
