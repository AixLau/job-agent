export const fallbackProfile = {
  account: "",
  email: "",
  fullName: "",
  phone: "",
  city: "",
  yearsExperience: null,
  summary: "",
  skills: [],
  profileStatus: "INCOMPLETE",
  updatedAt: "",
};

const resolveBaseUrl = (baseUrl) =>
  baseUrl || process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";

const normalizeProfile = (profile) => ({
  account: profile?.account ?? "",
  email: profile?.email ?? "",
  fullName: profile?.fullName ?? profile?.full_name ?? "",
  phone: profile?.phone ?? "",
  city: profile?.city ?? "",
  yearsExperience: profile?.yearsExperience ?? profile?.years_experience ?? null,
  summary: profile?.summary ?? "",
  skills: Array.isArray(profile?.skills) ? profile.skills : [],
  profileStatus: profile?.profileStatus ?? profile?.profile_status ?? "INCOMPLETE",
  updatedAt: profile?.updatedAt ?? profile?.updated_at ?? "",
});

export async function fetchProfile(baseUrl, token) {
  try {
    const response = await fetch(`${resolveBaseUrl(baseUrl)}/api/profile`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });
    if (!response.ok) {
      return fallbackProfile;
    }
    const data = await response.json();
    return normalizeProfile(data?.profile);
  } catch (error) {
    return fallbackProfile;
  }
}

export async function saveProfile(baseUrl, token, profile) {
  const response = await fetch(`${resolveBaseUrl(baseUrl)}/api/profile`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({
      full_name: profile.fullName ?? "",
      phone: profile.phone ?? "",
      city: profile.city ?? "",
      years_experience: profile.yearsExperience ?? null,
      summary: profile.summary ?? "",
      skills: Array.isArray(profile.skills) ? profile.skills : [],
    }),
  });
  if (!response.ok) {
    throw new Error("profile save failed");
  }
  const data = await response.json();
  return normalizeProfile(data?.profile);
}
