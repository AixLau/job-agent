export async function uploadResume(baseUrl, token, payload) {
  const resolvedBaseUrl =
    baseUrl || process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";
  const response = await fetch(`${resolvedBaseUrl}/api/resume`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(payload),
  });
  if (!response.ok) {
    throw new Error("resume upload failed");
  }
  return response.json();
}
