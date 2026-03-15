export const detectPageType = (url) => {
  if (!url) {
    return "list";
  }
  if (url.includes("job_detail")) {
    return "detail";
  }
  if (url.includes("chat")) {
    return "chat";
  }
  return "list";
};

export const extractJobPayload = () => {
  const url = typeof location !== "undefined" ? location.href : "";
  const pageType = detectPageType(url);
  if (pageType !== "detail" && pageType !== "list") {
    return {
      title: "",
      company: "",
      source: "",
      external_id: "",
    };
  }
  const titleEl = document.querySelector("h1");
  const companyEl = document.querySelector("[class*='company']");
  const title = titleEl?.textContent?.trim() || document.title || "未命名岗位";
  const company = companyEl?.textContent?.trim() || "未知公司";
  const externalIdMatch = url.match(/job_detail\/(\d+)/i);
  const externalId = externalIdMatch ? externalIdMatch[1] : hashText(url || "");
  return { title, company, source: "zhipin", external_id: externalId };
};

export const extractChatMessages = () => {
  const url = typeof location !== "undefined" ? location.href : "";
  if (detectPageType(url) !== "chat") {
    return [];
  }
  const nodes = Array.from(document.querySelectorAll("[class*='message']"));
  return nodes.map((node, index) => {
    const text = node.textContent?.trim() || "";
    const ts = node.getAttribute("data-ts") || new Date().toISOString();
    const id = hashText(`${index}:${text}:${ts}`);
    return { id, role: "hr", text, ts };
  });
};

export const hashText = (text) => {
  let hash = 0;
  for (let i = 0; i < text.length; i += 1) {
    hash = (hash * 31 + text.charCodeAt(i)) | 0;
  }
  return Math.abs(hash).toString(16);
};

if (typeof window !== "undefined") {
  window.JobAgentExtractor = {
    detectPageType,
    extractJobPayload,
    extractChatMessages,
    hashText,
  };
}
