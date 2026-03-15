export const fallbackSettings = {
  defaultAutomationLevel: "SEMI",
  autoSendEnabled: false,
  highRiskRequiresReview: true,
  chatImmediateAutoSend: false,
  dailyActionLimit: 30,
};

const normalizeSettings = (settings = {}) => ({
  defaultAutomationLevel: settings.defaultAutomationLevel ?? settings.default_automation_level ?? "SEMI",
  autoSendEnabled: settings.autoSendEnabled ?? settings.auto_send_enabled ?? false,
  highRiskRequiresReview:
    settings.highRiskRequiresReview ?? settings.high_risk_requires_review ?? true,
  chatImmediateAutoSend:
    settings.chatImmediateAutoSend ?? settings.chat_immediate_auto_send ?? false,
  dailyActionLimit: settings.dailyActionLimit ?? settings.daily_action_limit ?? 30,
});

export async function fetchSettings(baseUrl, token) {
  const resolvedBaseUrl =
    baseUrl || process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";
  try {
    const response = await fetch(`${resolvedBaseUrl}/api/settings`, {
      headers: {
        Authorization: token ? `Bearer ${token}` : "",
      },
      cache: "no-store",
    });
    if (!response.ok) {
      return fallbackSettings;
    }
    const data = await response.json();
    return normalizeSettings(data.settings ?? {});
  } catch (error) {
    return fallbackSettings;
  }
}

export async function saveSettings(baseUrl, token, settings) {
  const resolvedBaseUrl =
    baseUrl || process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";
  const response = await fetch(`${resolvedBaseUrl}/api/settings`, {
    method: "POST",
    headers: {
      Authorization: token ? `Bearer ${token}` : "",
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      default_automation_level: settings.defaultAutomationLevel,
      auto_send_enabled: settings.autoSendEnabled,
      high_risk_requires_review: settings.highRiskRequiresReview,
      chat_immediate_auto_send: settings.chatImmediateAutoSend,
      daily_action_limit: settings.dailyActionLimit,
    }),
  });
  if (!response.ok) {
    throw new Error("save settings failed");
  }
  const data = await response.json();
  return normalizeSettings(data.settings ?? {});
}
