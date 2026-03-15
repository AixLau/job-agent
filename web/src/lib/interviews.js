export const fallbackInterviews = [];

const normalizeInterview = (item = {}) => ({
  ...item,
  conversationId: item.conversationId ?? item.conversation_id ?? "",
  draftId: item.draftId ?? item.draft_id ?? "",
  draftContent: item.draftContent ?? item.draft_content ?? "",
  nextAction: item.nextAction ?? item.next_action ?? "",
  scheduledAt: item.scheduledAt ?? item.scheduled_at ?? "",
});

export async function fetchInterviews(baseUrl, token) {
  const resolvedBaseUrl =
    baseUrl || process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";
  try {
    const response = await fetch(`${resolvedBaseUrl}/api/interviews`, {
      headers: {
        Authorization: token ? `Bearer ${token}` : "",
      },
      cache: "no-store",
    });
    if (!response.ok) {
      return fallbackInterviews;
    }
    const data = await response.json();
    return Array.isArray(data.items) ? data.items.map(normalizeInterview) : fallbackInterviews;
  } catch (error) {
    return fallbackInterviews;
  }
}
