"use client";

import { useState } from "react";
import { login } from "../../lib/auth";

export default function LoginPage() {
  const [account, setAccount] = useState("");
  const [password, setPassword] = useState("");
  const [status, setStatus] = useState("");

  const onSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setStatus("Logging in...");
    try {
      const data = await login(undefined, { account, password });
      if (data?.access_token) {
        localStorage.setItem("access_token", data.access_token);
        setStatus("Logged in");
      } else {
        setStatus("Login failed");
      }
    } catch (error) {
      setStatus("Login failed");
    }
  };

  return (
    <main className="page">
      <div className="card">
        <div className="card-head">
          <h2>Login</h2>
        </div>
        <form className="form" onSubmit={onSubmit}>
          <label className="field">
            <span>Account</span>
            <input
              value={account}
              onChange={(event) => setAccount(event.target.value)}
              required
            />
          </label>
          <label className="field">
            <span>Password</span>
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
            />
          </label>
          <button type="submit">Login</button>
          <p className="hint">{status}</p>
        </form>
      </div>
    </main>
  );
}
