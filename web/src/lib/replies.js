import { fetchDashboard } from "./dashboard.js";

export const fallbackReplies = [];

export async function fetchReplies(baseUrl, token) {
  const data = await fetchDashboard(baseUrl, token);
  return Array.isArray(data.replies) ? data.replies : fallbackReplies;
}
