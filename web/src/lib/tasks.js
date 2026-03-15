export const fallbackTasks = [];
const knownCities = ["北京", "上海", "深圳", "广州", "杭州", "苏州", "成都", "武汉", "南京", "西安"];

export function parseTaskListInput(value) {
  if (Array.isArray(value)) {
    return value.map((item) => String(item).trim()).filter(Boolean);
  }
  if (!value) {
    return [];
  }
  return String(value)
    .split(/[，,\/、]|和/g)
    .map((item) => item.trim())
    .filter(Boolean);
}

export function normalizeTaskPayload(payload = {}) {
  return {
    title: payload.title ?? "",
    city: payload.city ?? "",
    salary: payload.salary ?? "",
    experience: payload.experience ?? "",
    exclude: parseTaskListInput(payload.exclude),
    preferences: parseTaskListInput(payload.preferences),
    automation_level: payload.automationLevel ?? payload.automation_level ?? "SEMI",
    strategy_text: payload.strategyText ?? payload.strategy_text ?? "",
  };
}

export function deriveTaskFormFromStrategy(strategy = {}) {
  return {
    title: strategy.title ?? "",
    city: strategy.city ?? "",
    salary: strategy.salary ?? "",
    experience: strategy.experience ?? "",
    automationLevel: strategy.automationLevel ?? "SEMI",
    exclude: Array.isArray(strategy.exclude) ? strategy.exclude.join(", ") : "",
    preferences: Array.isArray(strategy.preferences) ? strategy.preferences.join(", ") : "",
  };
}

export function parseStrategyText(strategyText = "") {
  const text = String(strategyText).trim();
  const city = knownCities.find((item) => text.includes(item)) ?? "";
  const salary = text.match(/(\d+\s*[kK](?:\s*-\s*\d+\s*[kK])?|\d+\s*[kK]\+)/)?.[1]?.replace(/\s+/g, "") ?? "";
  const experience =
    text.match(/(\d+\s*-\s*\d+\s*年|\d+\s*年以上)/)?.[1]?.replace(/\s+/g, "") ?? "";
  const automationLevel = text.includes("AUTO") || text.includes("自动")
    ? "AUTO"
    : text.includes("MANUAL") || text.includes("保守")
      ? "MANUAL"
      : "SEMI";
  const exclude = extractSegmentValues(text, "排除", ["偏好", "AUTO", "SEMI", "MANUAL", "自动", "半自动", "保守"]);
  const preferences = extractSegmentValues(text, "偏好", ["排除", "AUTO", "SEMI", "MANUAL", "自动", "半自动", "保守"]);

  let title = text;
  for (const token of [city, salary, experience, automationLevel, ...exclude, ...preferences]) {
    if (token) {
      title = title.replace(token, " ");
    }
  }
  title = title.replace(/排除/g, " ").replace(/偏好/g, " ").replace(/AUTO|SEMI|MANUAL|自动|半自动|保守/g, " ");
  title = title.replace(/\s+/g, " ").trim();

  return {
    title,
    city,
    salary,
    experience,
    automationLevel,
    exclude,
    preferences,
  };
}

export async function fetchTasks(baseUrl, token) {
  const resolvedBaseUrl =
    baseUrl || process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";
  try {
    const response = await fetch(`${resolvedBaseUrl}/api/tasks`, {
      headers: {
        Authorization: token ? `Bearer ${token}` : "",
      },
      cache: "no-store",
    });
    if (!response.ok) {
      return fallbackTasks;
    }
    const data = await response.json();
    return Array.isArray(data?.tasks) ? data.tasks : fallbackTasks;
  } catch (error) {
    return fallbackTasks;
  }
}

export async function createTask(baseUrl, token, payload) {
  const resolvedBaseUrl =
    baseUrl || process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";
  const normalizedPayload = normalizeTaskPayload(payload);
  const response = await fetch(`${resolvedBaseUrl}/api/tasks`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(normalizedPayload),
  });
  if (!response.ok) {
    throw new Error("create task failed");
  }
  return response.json();
}

function extractSegmentValues(text, label, stopTokens) {
  const marker = text.indexOf(label);
  if (marker < 0) {
    return [];
  }
  const segment = text.slice(marker + label.length);
  let stopIndex = segment.length;
  for (const token of stopTokens) {
    const nextIndex = segment.indexOf(token);
    if (nextIndex >= 0) {
      stopIndex = Math.min(stopIndex, nextIndex);
    }
  }
  return parseTaskListInput(segment.slice(0, stopIndex).trim().replace(/^[:：]/, ""));
}
