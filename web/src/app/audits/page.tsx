"use client";

import { useEffect, useMemo, useState } from "react";
import { fallbackAudits, fetchAudits } from "../../lib/audits";

export default function AuditsPage() {
  const [data, setData] = useState(fallbackAudits);
  const baseUrl = useMemo(
    () => process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080",
    []
  );

  useEffect(() => {
    const token = localStorage.getItem("access_token") || "";
    fetchAudits(baseUrl, token, 0, 10).then(setData);
  }, [baseUrl]);

  return (
    <main className="page">
      <header className="topbar">
        <div>
          <p className="eyebrow">Workbench</p>
          <h1>审计历史</h1>
          <p className="subtitle">查看插件动作、模型输出和风险标签。</p>
        </div>
        <div className="status-card">
          <p className="label">日志总数</p>
          <h2>{data.total}</h2>
          <p className="hint">第 {data.page + 1} 页</p>
        </div>
      </header>

      <section className="card">
        <div className="card-head">
          <h2>记录</h2>
          <span className="pill">{data.items.length}</span>
        </div>
        <div className="list">
          {data.items.map((item, index) => (
            <div className="list-item compact" key={`${item.actionType}-${index}`}>
              <div>
                <p className="title">{item.actionType || "UNKNOWN"}</p>
                <p className="muted">{item.payload || "{}"}</p>
                <p className="hint">
                  {Array.isArray(item.riskTags) && item.riskTags.length > 0
                    ? item.riskTags.join(" / ")
                    : "无风险标签"}
                </p>
              </div>
              <span className="tag ghost">
                {item.createdAt ? new Date(item.createdAt).toLocaleString() : "recent"}
              </span>
            </div>
          ))}
          {data.items.length === 0 ? (
            <div className="list-item compact">
              <div>
                <p className="title">暂无审计记录</p>
                <p className="muted">插件开始工作后，这里会显示动作历史。</p>
              </div>
            </div>
          ) : null}
        </div>
      </section>
    </main>
  );
}
