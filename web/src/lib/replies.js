export const fallbackReplies = [];

const normalizeReply = (item = {}) => ({
  ...item,
  conversationId: item.conversationId ?? item.conversation_id ?? "",
  jobPostId: item.jobPostId ?? item.job_post_id ?? "",
  nextAction: item.nextAction ?? item.next_action ?? "",
  priority: item.priority ?? "",
  followUpAt: item.followUpAt ?? item.follow_up_at ?? "",
  updatedAt: item.updatedAt ?? item.updated_at ?? "",
});

export async function fetchReplies(baseUrl, token) {
  const resolvedBaseUrl =
    baseUrl || process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";
  try {
    const response = await fetch(`${resolvedBaseUrl}/api/replies`, {
      headers: {
        Authorization: token ? `Bearer ${token}` : "",
      },
      cache: "no-store",
    });
    if (!response.ok) {
      return fallbackReplies;
    }
    const data = await response.json();
    return Array.isArray(data.items) ? data.items.map(normalizeReply) : fallbackReplies;
  } catch (error) {
    return fallbackReplies;
  }
}
