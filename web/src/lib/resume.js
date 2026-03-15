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

export async function parseResumeUpload(baseUrl, token, payload) {
  const resolvedBaseUrl =
    baseUrl || process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";
  const response = await fetch(`${resolvedBaseUrl}/api/resume/parse`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({
      content: payload.content,
      format: payload.format,
      source: payload.source,
      file_name: payload.fileName,
    }),
  });
  if (!response.ok) {
    throw new Error("resume parse failed");
  }
  return response.json();
}

export async function confirmResume(baseUrl, token, payload) {
  const resolvedBaseUrl =
    baseUrl || process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";
  const response = await fetch(`${resolvedBaseUrl}/api/resume/confirm`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({
      content: payload.content,
      format: payload.format,
      source: payload.source,
      file_name: payload.fileName,
      parsed_json: payload.parsedJson,
    }),
  });
  if (!response.ok) {
    throw new Error("resume confirm failed");
  }
  return response.json();
}
