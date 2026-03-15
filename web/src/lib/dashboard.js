export const fallbackDashboard = {
  metrics: {
    recommendations: 0,
    drafts: 0,
    replies: 0,
    interviews: 0,
  },
  recommendations: [],
  drafts: [],
  replies: [],
  interviews: [],
  updatedAt: "",
};

const normalizeRecommendation = (item) => ({
  ...item,
  jobPostId: item.jobPostId ?? item.job_post_id ?? "",
});

const normalizeDraft = (item) => ({
  ...item,
  draftId: item.draftId ?? item.draft_id ?? "",
  conversationId: item.conversationId ?? item.conversation_id ?? "",
  createdAt: item.createdAt ?? item.created_at ?? "",
});

const normalizeReply = (item) => ({
  ...item,
  conversationId: item.conversationId ?? item.conversation_id ?? "",
  updatedAt: item.updatedAt ?? item.updated_at ?? "",
});

const normalizeInterview = (item) => ({
  ...item,
  conversationId: item.conversationId ?? item.conversation_id ?? "",
  scheduledAt: item.scheduledAt ?? item.scheduled_at ?? "",
});

const normalizeDashboard = (data) => ({
  metrics: data.metrics ?? fallbackDashboard.metrics,
  recommendations: Array.isArray(data.recommendations)
    ? data.recommendations.map(normalizeRecommendation)
    : [],
  drafts: Array.isArray(data.drafts) ? data.drafts.map(normalizeDraft) : [],
  replies: Array.isArray(data.replies) ? data.replies.map(normalizeReply) : [],
  interviews: Array.isArray(data.interviews)
    ? data.interviews.map(normalizeInterview)
    : [],
  updatedAt: data.updatedAt ?? data.updated_at ?? "",
});

export async function fetchDashboard(baseUrl, token) {
  const resolvedBaseUrl =
    baseUrl || process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";
  try {
    const response = await fetch(`${resolvedBaseUrl}/api/dashboard`, {
      headers: {
        Authorization: token ? `Bearer ${token}` : "",
      },
      cache: "no-store",
    });
    if (!response.ok) {
      return fallbackDashboard;
    }
    const data = await response.json();
    return normalizeDashboard(data ?? {});
  } catch (error) {
    return fallbackDashboard;
  }
}
