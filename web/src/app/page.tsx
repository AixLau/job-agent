import { fetchDashboard, fallbackDashboard } from "../lib/dashboard";

export default async function Home() {
  const dashboard = await fetchDashboard();
  const metrics = dashboard.metrics ?? fallbackDashboard.metrics;
  const recommendations = dashboard.recommendations ?? [];
  const drafts = dashboard.drafts ?? [];
  const replies = dashboard.replies ?? [];

  const highlights = [
    { label: "今日新增岗位", value: String(metrics.recommendations) },
    { label: "待发送草稿", value: String(metrics.drafts) },
    { label: "待处理回复", value: String(metrics.replies) },
    { label: "面试邀约", value: String(metrics.interviews) },
  ];

  const hasRecommendations = recommendations.length > 0;
  const hasDrafts = drafts.length > 0;
  const hasReplies = replies.length > 0;

  const fallbackRecommendations = [
    { title: "资深产品经理", company: "智聘科技", score: 86, reasons: ["匹配度高，JD 清晰"] },
    { title: "B端产品负责人", company: "星火互娱", score: 82, reasons: ["经验要求匹配"] },
    { title: "运营策略专家", company: "云图", score: 79, reasons: ["薪资区间匹配"] },
  ];

  const fallbackDrafts = [
    { company: "智聘科技", title: "资深产品经理", content: "等待确认" },
    { company: "星火互娱", title: "B端产品负责人", content: "等待确认" },
  ];

  const fallbackReplies = [
    { company: "云图", intent: "面试邀约", summary: "1小时前" },
    { company: "山海数据", intent: "补充材料", summary: "3小时前" },
  ];

  const displayRecommendations = hasRecommendations ? recommendations : fallbackRecommendations;
  const displayDrafts = hasDrafts ? drafts : fallbackDrafts;
  const displayReplies = hasReplies ? replies : fallbackReplies;

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
                    {Array.isArray(item.reasons) ? item.reasons.join(" / ") : ""}
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
                <div className="list-item compact" key={item.company}>
                  <div>
                    <p className="title">{item.company}</p>
                    <p className="muted">{item.title}</p>
                  </div>
                  <span className="tag">待确认</span>
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
                <div className="list-item compact" key={item.company}>
                  <div>
                    <p className="title">{item.company}</p>
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
    </main>
  );
}
