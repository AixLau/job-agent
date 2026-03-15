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

export const buildOverlayHtml = ({ score, reasons, risks, draft }) => {
  const safeScore = escapeHtml(score ?? "-");
  const reasonHtml = renderList(reasons);
  const riskHtml = renderList(risks);
  const draftText = escapeHtml(draft?.content || "");
  return `
    <div id="job-agent-overlay" style="position:fixed;top:16px;right:16px;z-index:99999;width:300px;background:#fff;border:1px solid #ddd;border-radius:8px;box-shadow:0 8px 24px rgba(0,0,0,0.12);padding:12px;font-family:Arial,sans-serif;font-size:12px;">
      <div style="font-weight:700;font-size:14px;margin-bottom:8px;">Job Agent</div>
      <div><strong>Score:</strong> ${safeScore}</div>
      <div style="margin-top:8px;"><strong>Reasons</strong>${reasonHtml}</div>
      <div style="margin-top:8px;"><strong>Risks</strong>${riskHtml}</div>
      <div style="margin-top:8px;">
        <strong>Draft</strong>
        <textarea id="job-agent-draft" style="width:100%;height:80px;margin-top:4px;">${draftText}</textarea>
      </div>
      <div style="display:flex;gap:8px;margin-top:8px;">
        <button type="button" data-action="fill" style="flex:1;padding:6px;background:#1f6feb;color:#fff;border:none;border-radius:4px;cursor:pointer;">Fill</button>
        <button type="button" data-action="ignore" style="flex:1;padding:6px;background:#eee;border:none;border-radius:4px;cursor:pointer;">Ignore</button>
      </div>
    </div>
  `;
};

if (typeof window !== "undefined") {
  window.JobAgentUI = { buildOverlayHtml };
}

if (typeof module !== "undefined" && module.exports) {
  module.exports = { buildOverlayHtml };
}
