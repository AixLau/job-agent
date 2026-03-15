"use client";

import { useState } from "react";
import { uploadResume } from "../../lib/resume";

export default function ResumePage() {
  const [content, setContent] = useState("");
  const [status, setStatus] = useState("");

  const onSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const token = localStorage.getItem("access_token") || "";
    if (!token) {
      setStatus("Please login first");
      return;
    }
    setStatus("Uploading...");
    try {
      await uploadResume(undefined, token, {
        content,
        format: "TEXT",
        source: "manual",
      });
      setStatus("Uploaded");
    } catch (error) {
      setStatus("Upload failed");
    }
  };

  return (
    <main className="page">
      <div className="card">
        <div className="card-head">
          <h2>Resume</h2>
        </div>
        <form className="form" onSubmit={onSubmit}>
          <label className="field">
            <span>Content</span>
            <textarea
              value={content}
              onChange={(event) => setContent(event.target.value)}
              rows={8}
              required
            />
          </label>
          <button type="submit">Upload</button>
          <p className="hint">{status}</p>
        </form>
      </div>
    </main>
  );
}
