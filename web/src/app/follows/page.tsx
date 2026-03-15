"use client";

import { useEffect, useMemo, useState } from "react";
import { fallbackFollows, fetchFollows } from "../../lib/follows";

export default function FollowsPage() {
  const [data, setData] = useState(fallbackFollows);
  const baseUrl = useMemo(
    () => process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080",
    []
  );

  useEffect(() => {
    const token = localStorage.getItem("access_token") || "";
    fetchFollows(baseUrl, token, 0, 10).then(setData);
  }, [baseUrl]);

  return (
    <main className="page">
      <header className="topbar">
        <div>
          <p className="eyebrow">Workbench</p>
          <h1>关注列表</h1>
          <p className="subtitle">保留重点岗位，集中回看。</p>
        </div>
        <div className="status-card">
          <p className="label">总关注</p>
          <h2>{data.total}</h2>
          <p className="hint">第 {data.page + 1} 页</p>
        </div>
      </header>

      <section className="card">
        <div className="card-head">
          <h2>岗位</h2>
          <span className="pill">{data.items.length}</span>
        </div>
        <div className="list">
          {data.items.map((item) => (
            <div className="list-item compact" key={item.jobPostId || item.createdAt}>
              <div>
                <p className="title">{item.title || "未命名岗位"}</p>
                <p className="muted">{item.company || "未知公司"}</p>
              </div>
              <span className="tag ghost">
                {item.createdAt ? new Date(item.createdAt).toLocaleDateString() : "recent"}
              </span>
            </div>
          ))}
          {data.items.length === 0 ? (
            <div className="list-item compact">
              <div>
                <p className="title">暂无关注岗位</p>
                <p className="muted">在工作台点击“关注”后会出现在这里。</p>
              </div>
            </div>
          ) : null}
        </div>
      </section>
    </main>
  );
}
