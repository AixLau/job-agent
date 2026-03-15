"use client";

import { useEffect, useState } from "react";
import {
  createTask,
  deriveTaskFormFromStrategy,
  fetchTasks,
  parseStrategyText,
} from "../../lib/tasks";

export default function TasksPage() {
  const [tasks, setTasks] = useState([]);
  const [status, setStatus] = useState("");
  const [title, setTitle] = useState("");
  const [city, setCity] = useState("");
  const [salary, setSalary] = useState("");
  const [experience, setExperience] = useState("");
  const [automationLevel, setAutomationLevel] = useState("SEMI");
  const [strategyText, setStrategyText] = useState("");
  const [exclude, setExclude] = useState("");
  const [preferences, setPreferences] = useState("");

  useEffect(() => {
    const token = localStorage.getItem("access_token") || "";
    if (!token) {
      return;
    }
    fetchTasks(undefined, token).then(setTasks);
  }, []);

  const onSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const token = localStorage.getItem("access_token") || "";
    if (!token) {
      setStatus("Please login first");
      return;
    }
    setStatus("Creating...");
    try {
      await createTask(undefined, token, {
        title,
        city,
        salary,
        experience,
        automationLevel,
        strategyText,
        exclude,
        preferences,
      });
      const refreshed = await fetchTasks(undefined, token);
      setTasks(refreshed);
      setStatus("Created");
    } catch (error) {
      setStatus("Create failed");
    }
  };

  const onApplyStrategy = () => {
    const next = deriveTaskFormFromStrategy(parseStrategyText(strategyText));
    setTitle((current) => current || next.title);
    setCity((current) => current || next.city);
    setSalary((current) => current || next.salary);
    setExperience((current) => current || next.experience);
    setAutomationLevel((current) => current === "SEMI" ? next.automationLevel : current);
    setExclude((current) => current || next.exclude);
    setPreferences((current) => current || next.preferences);
    setStatus("Structured fields applied");
  };

  return (
    <main className="page">
      <div className="card">
        <div className="card-head">
          <h2>Create Task</h2>
        </div>
        <form className="form" onSubmit={onSubmit}>
          <label className="field">
            <span>Title</span>
            <input value={title} onChange={(e) => setTitle(e.target.value)} required />
          </label>
          <label className="field">
            <span>City</span>
            <input value={city} onChange={(e) => setCity(e.target.value)} />
          </label>
          <label className="field">
            <span>Salary</span>
            <input value={salary} onChange={(e) => setSalary(e.target.value)} />
          </label>
          <label className="field">
            <span>Experience</span>
            <input value={experience} onChange={(e) => setExperience(e.target.value)} />
          </label>
          <label className="field">
            <span>Automation Level</span>
            <select value={automationLevel} onChange={(e) => setAutomationLevel(e.target.value)}>
              <option value="MANUAL">保守模式</option>
              <option value="SEMI">半自动</option>
              <option value="AUTO">自动模式</option>
            </select>
          </label>
          <label className="field">
            <span>Exclude</span>
            <input
              value={exclude}
              onChange={(e) => setExclude(e.target.value)}
              placeholder="例如：外包, 派遣"
            />
          </label>
          <label className="field">
            <span>Preferences</span>
            <input
              value={preferences}
              onChange={(e) => setPreferences(e.target.value)}
              placeholder="例如：B端, 增长"
            />
          </label>
          <label className="field">
            <span>Strategy</span>
            <textarea
              rows={4}
              value={strategyText}
              onChange={(e) => setStrategyText(e.target.value)}
              placeholder="例如：上海 产品经理 20k-30k 3-5年 排除外包 偏好B端 AUTO"
            />
          </label>
          <button type="button" onClick={onApplyStrategy}>
            Apply Strategy
          </button>
          <button type="submit">Create</button>
          <p className="hint">{status}</p>
        </form>
      </div>

      <section className="card">
        <div className="card-head">
          <h2>Tasks</h2>
          <span className="pill">{tasks.length}</span>
        </div>
        <div className="list">
          {tasks.map((task) => (
            <div className="list-item compact" key={task.id ?? task.title}>
              <div>
                <p className="title">{task.title ?? "Untitled"}</p>
                <p className="muted">
                  {task.city ?? "N/A"} · {task.salary ?? "N/A"}
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
