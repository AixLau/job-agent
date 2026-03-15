"use client";

import { useEffect, useMemo, useState } from "react";
import { fetchDashboard, fallbackDashboard } from "../lib/dashboard";
import { fetchTasks } from "../lib/tasks";

export default function Home() {
  const [dashboard, setDashboard] = useState(fallbackDashboard);
  const [tasks, setTasks] = useState([]);
  const baseUrl = useMemo(
    () => process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080",
    []
  );

  useEffect(() => {
    const token = localStorage.getItem("access_token") || "";
    fetchDashboard(baseUrl, token).then(setDashboard);
    fetchTasks(baseUrl, token).then(setTasks);
  }, [baseUrl]);

  const metrics = dashboard.metrics ?? fallbackDashboard.metrics;
  const recommendations = dashboard.recommendations ?? [];
  const drafts = dashboard.drafts ?? [];
  const replies = dashboard.replies ?? [];
  const interviews = dashboard.interviews ?? [];
  const taskList = Array.isArray(tasks) ? tasks : [];

  const highlights = [
    { label: "今日新增岗位", value: String(metrics.recommendations) },
    { label: "待发送草稿", value: String(metrics.drafts) },
    { label: "待处理回复", value: String(metrics.replies) },
    { label: "面试邀约", value: String(metrics.interviews) },
  ];

  const hasRecommendations = recommendations.length > 0;
  const hasDrafts = drafts.length > 0;
  const hasReplies = replies.length > 0;
  const hasInterviews = interviews.length > 0;

  const fallbackRecommendations = [
    { title: "资深产品经理", company: "智聘科技", score: 86, risks: ["匹配度高，JD 清晰"] },
    { title: "B端产品负责人", company: "星火互娱", score: 82, risks: ["经验要求匹配"] },
    { title: "运营策略专家", company: "云图", score: 79, risks: ["薪资区间匹配"] },
  ];

  const fallbackDrafts = [
    { conversationId: "c1", content: "等待确认" },
    { conversationId: "c2", content: "等待确认" },
  ];

  const fallbackReplies = [
    { conversationId: "c3", intent: "INTERVIEW", summary: "1小时前" },
    { conversationId: "c4", intent: "NEEDS_REPLY", summary: "3小时前" },
  ];

  const fallbackInterviews = [
    { conversationId: "c5", company: "云图", title: "产品经理" },
  ];

  const displayRecommendations = hasRecommendations ? recommendations : fallbackRecommendations;
  const displayDrafts = hasDrafts ? drafts : fallbackDrafts;
  const displayReplies = hasReplies ? replies : fallbackReplies;
  const displayInterviews = hasInterviews ? interviews : fallbackInterviews;
  const displayTasks = taskList.length > 0
    ? taskList
    : [
        {
          id: "demo-task",
          status: "ACTIVE",
          title: "产品经理",
          city: "上海",
          salary: "20k-30k",
        },
      ];

  return (
    <main className="page">
      <header className="topbar">
        <div>
          <p className="eyebrow">Boss 直聘 · 半自动模式</p>
          <h1>Job Agent 工作台</h1>
          <p className="subtitle">聚焦关键节点，其余交给智能体。</p>
        </div>
        <div className="status-card">
          <p className="label">任务状态</p>
          <h2>ACTIVE</h2>
          <p className="hint">最近同步：2 分钟前</p>
        </div>
      </header>

      <section className="grid highlights">
        {highlights.map((item) => (
          <div className="card metric" key={item.label}>
            <p className="label">{item.label}</p>
            <h3>{item.value}</h3>
          </div>
        ))}
      </section>

      <section className="grid two-col">
        <div className="card">
          <div className="card-head">
            <h2>推荐岗位</h2>
            <span className="pill">实时评分</span>
          </div>
          <div className="list">
            {displayRecommendations.map((item) => (
              <div className="list-item" key={item.title}>
                <div>
                  <p className="title">{item.title}</p>
                  <p className="muted">{item.company}</p>
                  <p className="hint">
                    {Array.isArray(item.risks) ? item.risks.join(" / ") : ""}
                  </p>
                </div>
                <div className="score">{item.score}</div>
              </div>
            ))}
          </div>
        </div>

        <div className="stack">
          <div className="card">
            <div className="card-head">
              <h2>待发送草稿</h2>
              <span className="pill">{metrics.drafts}</span>
            </div>
            <div className="list">
              {displayDrafts.map((item) => (
                <div className="list-item compact" key={item.draftId ?? item.conversationId}>
                  <div>
                    <p className="title">{item.conversationId ?? "草稿"}</p>
                    <p className="muted">{item.content ?? ""}</p>
                  </div>
                  <span className="tag">{item.approved ? "已确认" : "待确认"}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="card">
            <div className="card-head">
              <h2>待处理回复</h2>
              <span className="pill warning">{metrics.replies}</span>
            </div>
            <div className="list">
              {displayReplies.map((item) => (
                <div className="list-item compact" key={item.conversationId}>
                  <div>
                    <p className="title">{item.conversationId}</p>
                    <p className="muted">{item.intent}</p>
                  </div>
                  <span className="tag ghost">
                    {item.summary ?? "待处理"}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      <section className="card">
        <div className="card-head">
          <h2>面试进展</h2>
          <span className="pill">{displayInterviews.length}</span>
        </div>
        <div className="list">
          {displayInterviews.map((item) => (
            <div className="list-item compact" key={item.conversationId}>
              <div>
                <p className="title">{item.company || "公司"}</p>
                <p className="muted">{item.title || "岗位"}</p>
              </div>
              <span className="tag">面试</span>
            </div>
          ))}
        </div>
      </section>

      <section className="card">
        <div className="card-head">
          <h2>当前任务</h2>
          <span className="pill">{displayTasks.length}</span>
        </div>
        <div className="list">
          {displayTasks.map((task) => (
            <div className="list-item compact" key={task.id ?? task.title}>
              <div>
                <p className="title">{task.title ?? "未命名任务"}</p>
                <p className="muted">
                  {task.city ?? "未知城市"} · {task.salary ?? "薪资未设定"}
                </p>
              </div>
              <span className="tag">{task.status ?? "ACTIVE"}</span>
            </div>
          ))}
        </div>
      </section>
    </main>
  );
}
