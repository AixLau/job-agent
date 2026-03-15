const resolveBaseUrl = (baseUrl) =>
  baseUrl || process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";

const postWithAuth = async (url, token, body) => {
  const response = await fetch(url, {
    method: "POST",
    headers: {
      Authorization: token ? `Bearer ${token}` : "",
      "Content-Type": "application/json",
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!response.ok) {
    throw new Error(`request failed: ${response.status}`);
  }
  return response.json();
};

export async function followJob(baseUrl, token, jobPostId) {
  return postWithAuth(`${resolveBaseUrl(baseUrl)}/api/jobs/${jobPostId}/follow`, token);
}

export async function ignoreJob(baseUrl, token, jobPostId) {
  return postWithAuth(`${resolveBaseUrl(baseUrl)}/api/jobs/${jobPostId}/ignore`, token);
}

export async function blacklistCompany(baseUrl, token, { companyName, source }) {
  return postWithAuth(`${resolveBaseUrl(baseUrl)}/api/blacklist/company`, token, {
    company_name: companyName,
    source,
  });
}
