export const fallbackFollows = {
  items: [],
  page: 0,
  size: 10,
  total: 0,
};

const normalizeFollow = (item) => ({
  ...item,
  jobPostId: item.jobPostId ?? item.job_post_id ?? "",
  createdAt: item.createdAt ?? item.created_at ?? "",
});

export async function fetchFollows(baseUrl, token, page = 0, size = 10) {
  const resolvedBaseUrl =
    baseUrl || process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";
  try {
    const response = await fetch(
      `${resolvedBaseUrl}/api/follows?page=${page}&size=${size}`,
      {
        headers: {
          Authorization: token ? `Bearer ${token}` : "",
        },
        cache: "no-store",
      }
    );
    if (!response.ok) {
      return fallbackFollows;
    }
    const data = await response.json();
    return {
      items: Array.isArray(data.items) ? data.items.map(normalizeFollow) : [],
      page: data.page ?? 0,
      size: data.size ?? size,
      total: data.total ?? 0,
    };
  } catch (error) {
    return fallbackFollows;
  }
}
