import { fetchDashboard } from "./dashboard.js";

export const fallbackInterviews = [];

export async function fetchInterviews(baseUrl, token) {
  const data = await fetchDashboard(baseUrl, token);
  return Array.isArray(data.interviews) ? data.interviews : fallbackInterviews;
}
