"use client";

import { useEffect, useMemo, useState } from "react";
import { fallbackRecommendations, fetchRecommendations } from "../../lib/recommendations";

export default function RecommendationsPage() {
  const [items, setItems] = useState(fallbackRecommendations);
  const baseUrl = useMemo(
    () => process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080",
    []
  );

  useEffect(() => {
    const token = localStorage.getItem("access_token") || "";
    fetchRecommendations(baseUrl, token).then(setItems);
  }, [baseUrl]);

  return (
    <main className="page">
      <header className="topbar">
        <div>
          <p className="eyebrow">Workbench</p>
          <h1>推荐岗位</h1>
          <p className="subtitle">每条推荐都展示匹配理由与风险。</p>
        </div>
      </header>
      <section className="card">
        <div className="card-head">
          <h2>列表</h2>
          <span className="pill">{items.length}</span>
        </div>
        <div className="list">
          {items.map((item) => (
            <div className="list-item" key={item.jobPostId || item.title}>
              <div>
                <p className="title">{item.title}</p>
                <p className="muted">{item.company}</p>
                <p className="muted">{Array.isArray(item.reasons) ? item.reasons.join(" / ") : "无推荐理由"}</p>
                <p className="hint">{Array.isArray(item.risks) ? item.risks.join(" / ") : "无风险标签"}</p>
              </div>
              <div className="score">{item.score}</div>
            </div>
          ))}
        </div>
      </section>
    </main>
  );
}
