export const fallbackTasks = [];

export async function fetchTasks(baseUrl) {
  const resolvedBaseUrl =
    baseUrl || process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";
  try {
    const response = await fetch(`${resolvedBaseUrl}/api/tasks`, {
      cache: "no-store",
    });
    if (!response.ok) {
      return fallbackTasks;
    }
    const data = await response.json();
    return Array.isArray(data) ? data : fallbackTasks;
  } catch (error) {
    return fallbackTasks;
  }
}
