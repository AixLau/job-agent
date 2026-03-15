export const fallbackAudits = {
  items: [],
  page: 0,
  size: 10,
  total: 0,
};

const normalizeAudit = (item) => ({
  ...item,
  actionType: item.actionType ?? item.action_type ?? "",
  createdAt: item.createdAt ?? item.created_at ?? "",
  modelOutput: item.modelOutput ?? item.model_output ?? null,
  riskTags: item.riskTags ?? item.risk_tags ?? [],
});

export async function fetchAudits(baseUrl, token, page = 0, size = 10) {
  const resolvedBaseUrl =
    baseUrl || process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";
  try {
    const response = await fetch(
      `${resolvedBaseUrl}/api/audits?page=${page}&size=${size}`,
      {
        headers: {
          Authorization: token ? `Bearer ${token}` : "",
        },
        cache: "no-store",
      }
    );
    if (!response.ok) {
      return fallbackAudits;
    }
    const data = await response.json();
    return {
      items: Array.isArray(data.items) ? data.items.map(normalizeAudit) : [],
      page: data.page ?? 0,
      size: data.size ?? size,
      total: data.total ?? 0,
    };
  } catch (error) {
    return fallbackAudits;
  }
}
