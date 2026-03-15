export const fallbackTasks = [];

export async function fetchTasks(baseUrl, token) {
  const resolvedBaseUrl =
    baseUrl || process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";
  try {
    const response = await fetch(`${resolvedBaseUrl}/api/tasks`, {
      headers: {
        Authorization: token ? `Bearer ${token}` : "",
      },
      cache: "no-store",
    });
    if (!response.ok) {
      return fallbackTasks;
    }
    const data = await response.json();
    return Array.isArray(data?.tasks) ? data.tasks : fallbackTasks;
  } catch (error) {
    return fallbackTasks;
  }
}

export async function createTask(baseUrl, token, payload) {
  const resolvedBaseUrl =
    baseUrl || process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";
  const response = await fetch(`${resolvedBaseUrl}/api/tasks`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(payload),
  });
  if (!response.ok) {
    throw new Error("create task failed");
  }
  return response.json();
}
