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
  if (pageType === "list") {
    return {
      title,
      company,
      source: "zhipin",
      external_id: externalId,
      cards: extractJobCards(),
    };
  }
  return {
    title,
    company,
    source: "zhipin",
    external_id: externalId,
    salary: textFromSelectors(["[class*='salary']", "[data-field='salary']"]),
    experience: textFromSelectors(["[class*='experience']", "[data-field='experience']"]),
    city: textFromSelectors(["[class*='city']", "[data-field='city']"]),
    jd_raw: document.body?.innerText?.trim() || "",
  };
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
    return { id, role: detectMessageRole(node), text, ts };
  });
};

const extractJobCards = () => {
  const nodes = Array.from(
    document.querySelectorAll("[class*='job-card'], [data-job-id], .job-card")
  );
  return nodes.map((node) => {
    const link = node.querySelector?.("a");
    const url = link?.href || "";
    const externalId =
      node.getAttribute?.("data-job-id") ||
      url.match(/job_detail\/(\d+)/i)?.[1] ||
      hashText(url || node.textContent || "");
    return {
      title:
        textFromNode(node, ["h2", "[class*='title']", "[data-field='title']"]) ||
        "未命名岗位",
      company:
        textFromNode(node, ["[class*='company']", "[data-field='company']"]) ||
        "未知公司",
      salary: textFromNode(node, ["[class*='salary']", "[data-field='salary']"]) || "",
      experience:
        textFromNode(node, ["[class*='experience']", "[data-field='experience']"]) || "",
      city: textFromNode(node, ["[class*='city']", "[data-field='city']"]) || "",
      external_id: externalId,
      url,
    };
  });
};

const detectMessageRole = (node) => {
  const dataRole = node.getAttribute?.("data-role") || node.dataset?.role || "";
  const className = typeof node.className === "string" ? node.className.toLowerCase() : "";
  const hint = String(dataRole).toLowerCase();
  if (hint.includes("user") || className.includes("from-user") || className.includes("self")) {
    return "user";
  }
  if (hint.includes("hr") || className.includes("from-hr") || className.includes("boss")) {
    return "hr";
  }
  return "hr";
};

const textFromSelectors = (selectors) => {
  for (const selector of selectors) {
    const text = document.querySelector(selector)?.textContent?.trim();
    if (text) {
      return text;
    }
  }
  return "";
};

const textFromNode = (node, selectors) => {
  for (const selector of selectors) {
    const text = node.querySelector?.(selector)?.textContent?.trim();
    if (text) {
      return text;
    }
  }
  return "";
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
