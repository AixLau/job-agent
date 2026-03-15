"use client";

import { useState } from "react";
import { confirmResume, parseResumeUpload, uploadResume } from "../../lib/resume";

export default function ResumePage() {
  const [content, setContent] = useState("");
  const [status, setStatus] = useState("");
  const [fileName, setFileName] = useState("");
  const [format, setFormat] = useState("TEXT");
  const [parsedJson, setParsedJson] = useState(null);

  const onSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const token = localStorage.getItem("access_token") || "";
    if (!token) {
      setStatus("Please login first");
      return;
    }
    setStatus("Uploading...");
    try {
      if (format === "TEXT") {
        await uploadResume(undefined, token, {
          content,
          format: "TEXT",
          source: "manual",
        });
        setParsedJson(null);
        setStatus("Uploaded");
        return;
      }
      const preview = await parseResumeUpload(undefined, token, {
        content,
        fileName,
        format,
        source: "upload",
      });
      setParsedJson(preview.parsed_json);
      setStatus("Parsed");
    } catch (error) {
      setStatus("Upload failed");
    }
  };

  const onConfirm = async () => {
    const token = localStorage.getItem("access_token") || "";
    if (!token || !parsedJson) {
      return;
    }
    setStatus("Confirming...");
    try {
      await confirmResume(undefined, token, {
        content,
        format,
        source: "upload",
        fileName,
        parsedJson,
      });
      setStatus("Confirmed");
    } catch (error) {
      setStatus("Confirm failed");
    }
  };

  const onFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }
    setFileName(file.name);
    const ext = file.name.split(".").pop()?.toUpperCase();
    if (ext === "PDF" || ext === "DOCX") {
      setFormat(ext);
    }
    const text = await file.text();
    setContent(text);
  };

  return (
    <main className="page">
      <div className="card">
        <div className="card-head">
          <h2>Resume</h2>
        </div>
        <form className="form" onSubmit={onSubmit}>
          <label className="field">
            <span>Resume File</span>
            <input type="file" accept=".pdf,.doc,.docx,.txt" onChange={onFileChange} />
          </label>
          <label className="field">
            <span>Format</span>
            <input value={format} onChange={(event) => setFormat(event.target.value)} />
          </label>
          <label className="field">
            <span>Content</span>
            <textarea
              value={content}
              onChange={(event) => setContent(event.target.value)}
              rows={8}
              required
            />
          </label>
          <button type="submit">{format === "TEXT" ? "Upload" : "Parse"}</button>
          {parsedJson ? (
            <>
              <pre className="hint">{JSON.stringify(parsedJson, null, 2)}</pre>
              <button type="button" onClick={onConfirm}>Confirm</button>
            </>
          ) : null}
          <p className="hint">{status}</p>
        </form>
      </div>
    </main>
  );
}
