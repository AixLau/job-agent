"use client";

import { useEffect, useMemo, useState } from "react";
import { fallbackSettings, fetchSettings, saveSettings } from "../../lib/settings";

export default function SettingsPage() {
  const [settings, setSettings] = useState(fallbackSettings);
  const [status, setStatus] = useState("");
  const baseUrl = useMemo(
    () => process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080",
    []
  );

  useEffect(() => {
    const token = localStorage.getItem("access_token") || "";
    fetchSettings(baseUrl, token).then(setSettings);
  }, [baseUrl]);

  const onSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const token = localStorage.getItem("access_token") || "";
    try {
      const next = await saveSettings(baseUrl, token, settings);
      setSettings(next);
      setStatus("设置已保存");
    } catch (error) {
      setStatus("保存失败");
    }
  };

  return (
    <main className="page">
      <header className="topbar">
        <div>
          <p className="eyebrow">Workbench</p>
          <h1>设置</h1>
          <p className="subtitle">配置自动化阈值与聊天页即时发送策略。</p>
        </div>
      </header>
      <section className="card">
        <form className="form" onSubmit={onSubmit}>
          <label className="field">
            <span>默认自动化等级</span>
            <select
              value={settings.defaultAutomationLevel}
              onChange={(e) => setSettings({ ...settings, defaultAutomationLevel: e.target.value })}
            >
              <option value="MANUAL">保守</option>
              <option value="SEMI">半自动</option>
              <option value="AUTO">自动</option>
            </select>
          </label>
          <label className="field checkbox">
            <span>开启自动发送</span>
            <input
              type="checkbox"
              checked={settings.autoSendEnabled}
              onChange={(e) => setSettings({ ...settings, autoSendEnabled: e.target.checked })}
            />
          </label>
          <label className="field checkbox">
            <span>高风险仍需人工确认</span>
            <input
              type="checkbox"
              checked={settings.highRiskRequiresReview}
              onChange={(e) => setSettings({ ...settings, highRiskRequiresReview: e.target.checked })}
            />
          </label>
          <label className="field checkbox">
            <span>聊天页立即自动发送</span>
            <input
              type="checkbox"
              checked={settings.chatImmediateAutoSend}
              onChange={(e) => setSettings({ ...settings, chatImmediateAutoSend: e.target.checked })}
            />
          </label>
          <label className="field">
            <span>每日动作上限</span>
            <input
              type="number"
              value={settings.dailyActionLimit}
              onChange={(e) => setSettings({ ...settings, dailyActionLimit: Number(e.target.value) })}
            />
          </label>
          <button type="submit">保存设置</button>
          <p className="hint">{status}</p>
        </form>
      </section>
    </main>
  );
}
