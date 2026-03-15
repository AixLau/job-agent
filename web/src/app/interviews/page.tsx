"use client";

import { useEffect, useMemo, useState } from "react";
import { fallbackInterviews, fetchInterviews } from "../../lib/interviews";

export default function InterviewsPage() {
  const [items, setItems] = useState(fallbackInterviews);
  const baseUrl = useMemo(
    () => process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080",
    []
  );

  useEffect(() => {
    const token = localStorage.getItem("access_token") || "";
    fetchInterviews(baseUrl, token).then(setItems);
  }, [baseUrl]);

  return (
    <main className="page">
      <header className="topbar">
        <div>
          <p className="eyebrow">Workbench</p>
          <h1>面试机会</h1>
          <p className="subtitle">集中查看已进入面试阶段的机会。</p>
        </div>
      </header>
      <section className="card">
        <div className="card-head">
          <h2>面试列表</h2>
          <span className="pill">{items.length}</span>
        </div>
        <div className="list">
          {items.map((item) => (
            <div className="list-item compact" key={item.conversationId}>
              <div>
                <p className="title">{item.company}</p>
                <p className="muted">{item.title}</p>
              </div>
              <span className="tag">
                {item.scheduledAt ? new Date(item.scheduledAt).toLocaleDateString() : "待安排"}
              </span>
            </div>
          ))}
        </div>
      </section>
    </main>
  );
}
