console.debug("Job Agent content script loaded");

const MAX_TEXT_LENGTH = 5000;

const extractor = window.JobAgentExtractor || {};
const {
  detectPageType,
  extractJobPayload,
  extractChatMessages,
  hashText,
} = extractor;
const { buildOverlayHtml } = window.JobAgentUI || {};

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
