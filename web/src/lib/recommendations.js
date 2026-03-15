import { fetchDashboard } from "./dashboard.js";

export const fallbackRecommendations = [];

export async function fetchRecommendations(baseUrl, token) {
  const data = await fetchDashboard(baseUrl, token);
  return Array.isArray(data.recommendations) ? data.recommendations : fallbackRecommendations;
}
