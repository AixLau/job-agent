"use client";

import { useEffect, useMemo, useState } from "react";

import { fallbackProfile, fetchProfile, saveProfile } from "../../lib/profile";

export default function ProfilePage() {
  const [profile, setProfile] = useState(fallbackProfile);
  const [status, setStatus] = useState("");
  const [skillsInput, setSkillsInput] = useState("");
  const baseUrl = useMemo(
    () => process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080",
    []
  );

  useEffect(() => {
    const token = localStorage.getItem("access_token") || "";
    if (!token) {
      return;
    }
    fetchProfile(baseUrl, token).then((data) => {
      setProfile(data);
      setSkillsInput(Array.isArray(data.skills) ? data.skills.join(", ") : "");
    });
  }, [baseUrl]);

  const updateField = (field, value) => {
    setProfile((current) => ({ ...current, [field]: value }));
  };

  const onSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const token = localStorage.getItem("access_token") || "";
    if (!token) {
      setStatus("Please login first");
      return;
    }
    setStatus("Saving...");
    try {
      const saved = await saveProfile(baseUrl, token, {
        ...profile,
        skills: skillsInput
          .split(",")
          .map((item) => item.trim())
          .filter(Boolean),
      });
      setProfile(saved);
      setSkillsInput(saved.skills.join(", "));
      setStatus("Saved");
    } catch (error) {
      setStatus("Save failed");
    }
  };

  return (
    <main className="page">
      <div className="card">
        <div className="card-head">
          <h2>Profile</h2>
        </div>
        <form className="form" onSubmit={onSubmit}>
          <label className="field">
            <span>Account</span>
            <input value={profile.account} readOnly />
          </label>
          <label className="field">
            <span>Email</span>
            <input value={profile.email} readOnly />
          </label>
          <label className="field">
            <span>Full Name</span>
            <input
              value={profile.fullName}
              onChange={(event) => updateField("fullName", event.target.value)}
            />
          </label>
          <label className="field">
            <span>Phone</span>
            <input
              value={profile.phone}
              onChange={(event) => updateField("phone", event.target.value)}
            />
          </label>
          <label className="field">
            <span>City</span>
            <input
              value={profile.city}
              onChange={(event) => updateField("city", event.target.value)}
            />
          </label>
          <label className="field">
            <span>Years Experience</span>
            <input
              type="number"
              value={profile.yearsExperience ?? ""}
              onChange={(event) => {
                const value = event.target.value;
                updateField("yearsExperience", value ? Number(value) : null);
              }}
            />
          </label>
          <label className="field">
            <span>Skills</span>
            <input
              value={skillsInput}
              onChange={(event) => setSkillsInput(event.target.value)}
              placeholder="PRD, Growth, Java"
            />
          </label>
          <label className="field">
            <span>Summary</span>
            <textarea
              rows={4}
              value={profile.summary}
              onChange={(event) => updateField("summary", event.target.value)}
            />
          </label>
          <button type="submit">Save</button>
          <p className="hint">{status || `Profile: ${profile.profileStatus}`}</p>
        </form>
      </div>
    </main>
  );
}
