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
};

export async function fetchDashboard(baseUrl) {
  const resolvedBaseUrl =
    baseUrl || process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";
  try {
    const response = await fetch(`${resolvedBaseUrl}/api/dashboard`, {
      cache: "no-store",
    });
    if (!response.ok) {
      return fallbackDashboard;
    }
    const data = await response.json();
    return data;
  } catch (error) {
    return fallbackDashboard;
  }
}
