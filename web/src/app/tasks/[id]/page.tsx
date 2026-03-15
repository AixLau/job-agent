"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { fetchTasks } from "../../../lib/tasks";

export default function TaskDetailPage({ params }: { params: { id: string } }) {
  const [task, setTask] = useState<any>(null);
  const baseUrl = useMemo(
    () => process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080",
    []
  );

  useEffect(() => {
    const token = localStorage.getItem("access_token") || "";
    fetchTasks(baseUrl, token).then((items) => {
      setTask(items.find((item) => item.id === params.id) || null);
    });
  }, [baseUrl, params.id]);

  return (
    <main className="page">
      <header className="topbar">
        <div>
          <p className="eyebrow">Task Detail</p>
          <h1>{task?.title || "任务详情"}</h1>
          <p className="subtitle">{task?.city || "未设置城市"} · {task?.salary || "未设置薪资"}</p>
        </div>
        <Link href="/tasks">返回任务列表</Link>
      </header>

      <section className="card">
        <div className="list">
          <div className="list-item compact">
            <div>
              <p className="title">自动化等级</p>
              <p className="muted">{task?.automation_level || task?.automationLevel || "SEMI"}</p>
            </div>
          </div>
          <div className="list-item compact">
            <div>
              <p className="title">策略 JSON</p>
              <p className="muted">{task?.strategy_json || task?.strategyJson || "{}"}</p>
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}
