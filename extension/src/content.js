console.debug("Job Agent content script loaded");

const MAX_TEXT_LENGTH = 5000;

const hashText = (text) => {
  let hash = 0;
  for (let i = 0; i < text.length; i += 1) {
    hash = (hash * 31 + text.charCodeAt(i)) | 0;
  }
  return Math.abs(hash).toString(16);
};

const extractText = () => {
  const raw = document.body?.innerText ?? "";
  return raw.length > MAX_TEXT_LENGTH ? raw.slice(0, MAX_TEXT_LENGTH) : raw;
};

const extractPageInfo = () => {
  const titleEl = document.querySelector("h1");
  const companyEl =
    document.querySelector("[class*='company'] a") ||
    document.querySelector("[class*='company']");
  const title = titleEl?.textContent?.trim() || document.title || "未命名岗位";
  const company = companyEl?.textContent?.trim() || "未知公司";

  const rawText = extractText();
  return {
    page_type: location.href.includes("job_detail") ? "detail" : "list",
    raw_text: rawText,
    extracted_json: {
      title,
      company,
    },
    source_url: location.href,
    dom_hash: hashText(rawText + title + company),
  };
};

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message?.type === "PING") {
    sendResponse({ type: "PONG", source: "content" });
    return true;
  }
  if (message?.type === "EXTRACT_PAGE") {
    sendResponse(extractPageInfo());
    return true;
  }
  return false;
});
