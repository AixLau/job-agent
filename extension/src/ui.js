const escapeHtml = (value) => {
  const text = value == null ? "" : String(value);
  return text
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
};

const renderList = (items) => {
  if (!items || !items.length) {
    return "<div class=\"job-agent-empty\">-</div>";
  }
  const listItems = items
    .map((item) => `<li>${escapeHtml(item)}</li>`)
    .join("");
  return `<ul>${listItems}</ul>`;
};

const renderModeHint = ({ autoSend, auto_send: autoSendSnake, requiresReview, requires_review: requiresReviewSnake, reply }) => {
  const reviewRequired = Boolean(requiresReview || requiresReviewSnake);
  const shouldAutoSend = Boolean(autoSend || autoSendSnake);
  if (reviewRequired) {
    return "高风险需人工确认";
  }
  if (shouldAutoSend) {
    return "满足自动发送条件";
  }
  if (reply?.next_action || reply?.nextAction) {
    return `建议动作：${escapeHtml(reply?.next_action || reply?.nextAction)}`;
  }
  return "可先填充草稿，再决定是否发送";
};

export const buildOverlayHtml = ({
  score,
  reasons,
  risks,
  draft,
  reply,
  autoSend,
  auto_send: autoSendSnake,
  requiresReview,
  requires_review: requiresReviewSnake,
}) => {
  const safeScore = escapeHtml(score ?? "-");
  const reasonHtml = renderList(reasons);
  const riskHtml = renderList(risks);
  const draftText = escapeHtml(draft?.content || "");
  const hintText = renderModeHint({
    autoSend,
    auto_send: autoSendSnake,
    requiresReview,
    requires_review: requiresReviewSnake,
    reply,
  });
  const reviewRequired = Boolean(requiresReview || requiresReviewSnake);
  return `
    <div id="job-agent-overlay" style="position:fixed;top:16px;right:16px;z-index:99999;width:340px;background:#fff;border:1px solid #ddd;border-radius:12px;box-shadow:0 12px 28px rgba(0,0,0,0.16);padding:14px;font-family:Arial,sans-serif;font-size:12px;">
      <div style="display:flex;justify-content:space-between;align-items:center;gap:8px;">
        <div style="font-weight:700;font-size:14px;">Job Agent</div>
        <button type="button" data-action="close" style="border:none;background:transparent;color:#666;cursor:pointer;">关闭</button>
      </div>
      <div style="margin-top:6px;"><strong>Score:</strong> ${safeScore}</div>
      <div style="margin-top:6px;color:${reviewRequired ? "#c2410c" : "#4b5563"};">${hintText}</div>
      <div style="margin-top:8px;"><strong>Reasons</strong>${reasonHtml}</div>
      <div style="margin-top:8px;"><strong>Risks</strong>${riskHtml}</div>
      <div style="margin-top:8px;">
        <strong>Draft</strong>
        <textarea id="job-agent-draft" style="width:100%;height:80px;margin-top:4px;">${draftText}</textarea>
      </div>
      <div style="display:grid;grid-template-columns:repeat(2, minmax(0, 1fr));gap:8px;margin-top:10px;">
        <button type="button" data-action="fill" style="padding:6px;background:#1f6feb;color:#fff;border:none;border-radius:6px;cursor:pointer;">填充</button>
        <button type="button" data-action="send" style="padding:6px;background:#111827;color:#fff;border:none;border-radius:6px;cursor:pointer;">发送</button>
        <button type="button" data-action="pause" style="padding:6px;background:#f59e0b;color:#111827;border:none;border-radius:6px;cursor:pointer;">暂停自动化</button>
        <button type="button" data-action="ignore" style="padding:6px;background:#eee;border:none;border-radius:6px;cursor:pointer;">忽略</button>
      </div>
    </div>
  `;
};

export const performAutoSend = async ({
  draftText,
  inputEl,
  sendButtonEl,
  reportAction,
  taskId,
  afterSend,
  requiresReview,
}) => {
  const report = typeof reportAction === "function" ? reportAction : async () => {};
  if (requiresReview) {
    await report({
      task_id: taskId || "",
      action_type: "REVIEW_REQUIRED",
      status: "review_required",
      payload: { draft: draftText || "" },
    });
    return { status: "review_required" };
  }
  if (!draftText || !inputEl || !sendButtonEl) {
    await report({
      task_id: taskId || "",
      action_type: "FAILED",
      status: "failed",
      payload: { reason: "missing_input_or_button" },
    });
    return { status: "failed" };
  }

  if ("value" in inputEl) {
    inputEl.value = draftText;
  } else {
    inputEl.textContent = draftText;
  }
  if (typeof inputEl.dispatchEvent === "function") {
    inputEl.dispatchEvent(new Event("input", { bubbles: true }));
  }

  await report({
    task_id: taskId || "",
    action_type: "SEND",
    status: "sent",
    payload: { draft: draftText },
  });

  if (typeof sendButtonEl.click === "function") {
    sendButtonEl.click();
  }

  if (typeof afterSend === "function") {
    await afterSend();
  }

  await report({
    task_id: taskId || "",
    action_type: "DELIVERED",
    status: "delivered",
    payload: { draft: draftText },
  });

  return { status: "ok" };
};

if (typeof window !== "undefined") {
  window.JobAgentUI = { buildOverlayHtml, performAutoSend };
}

if (typeof module !== "undefined" && module.exports) {
  module.exports = { buildOverlayHtml, performAutoSend };
}
