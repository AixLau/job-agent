"use client";

import { useEffect, useMemo, useState } from "react";
import { fallbackReplies, fetchReplies } from "../../lib/replies";

export default function RepliesPage() {
  const [items, setItems] = useState(fallbackReplies);
  const baseUrl = useMemo(
    () => process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080",
    []
  );

  useEffect(() => {
    const token = localStorage.getItem("access_token") || "";
    fetchReplies(baseUrl, token).then(setItems);
  }, [baseUrl]);

  return (
    <main className="page">
      <header className="topbar">
        <div>
          <p className="eyebrow">Workbench</p>
          <h1>待处理回复</h1>
          <p className="subtitle">优先处理高价值对话与面试推进。</p>
        </div>
      </header>
      <section className="card">
        <div className="card-head">
          <h2>回复列表</h2>
          <span className="pill warning">{items.length}</span>
        </div>
        <div className="list">
          {items.map((item) => (
            <div className="list-item compact" key={item.conversationId}>
              <div>
                <p className="title">{item.company || item.conversationId}</p>
                <p className="muted">{item.intent}</p>
                <p className="hint">{item.summary}</p>
              </div>
              <span className="tag ghost">
                {item.updatedAt ? new Date(item.updatedAt).toLocaleString() : "recent"}
              </span>
            </div>
          ))}
        </div>
      </section>
    </main>
  );
}
