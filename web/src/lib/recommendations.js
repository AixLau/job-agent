export const fallbackRecommendations = [];

const normalizeRecommendation = (item = {}) => ({
  ...item,
  jobPostId: item.jobPostId ?? item.job_post_id ?? "",
  reasons: Array.isArray(item.reasons) ? item.reasons : [],
  risks: Array.isArray(item.risks) ? item.risks : [],
});

export async function fetchRecommendations(baseUrl, token) {
  const resolvedBaseUrl =
    baseUrl || process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";
  try {
    const response = await fetch(`${resolvedBaseUrl}/api/recommendations`, {
      headers: {
        Authorization: token ? `Bearer ${token}` : "",
      },
      cache: "no-store",
    });
    if (!response.ok) {
      return fallbackRecommendations;
    }
    const data = await response.json();
    return Array.isArray(data.items) ? data.items.map(normalizeRecommendation) : fallbackRecommendations;
  } catch (error) {
    return fallbackRecommendations;
  }
}
