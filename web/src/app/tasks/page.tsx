"use client";

import { useEffect, useState } from "react";
import { createTask, fetchTasks } from "../../lib/tasks";

export default function TasksPage() {
  const [tasks, setTasks] = useState([]);
  const [status, setStatus] = useState("");
  const [title, setTitle] = useState("");
  const [city, setCity] = useState("");
  const [salary, setSalary] = useState("");
  const [experience, setExperience] = useState("");
  const [automationLevel, setAutomationLevel] = useState("SEMI");
  const [strategyText, setStrategyText] = useState("");

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
        automation_level: automationLevel,
        strategy_text: strategyText,
        exclude: [],
        preferences: [],
      });
      const refreshed = await fetchTasks(undefined, token);
      setTasks(refreshed);
      setStatus("Created");
    } catch (error) {
      setStatus("Create failed");
    }
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
            <input
              value={automationLevel}
              onChange={(e) => setAutomationLevel(e.target.value)}
            />
          </label>
          <label className="field">
            <span>Strategy</span>
            <textarea
              rows={4}
              value={strategyText}
              onChange={(e) => setStrategyText(e.target.value)}
            />
          </label>
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
