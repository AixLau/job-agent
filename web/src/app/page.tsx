const highlights = [
  { label: "今日新增岗位", value: "18" },
  { label: "待发送草稿", value: "6" },
  { label: "待处理回复", value: "3" },
  { label: "面试邀约", value: "1" },
];

const recommendations = [
  { title: "资深产品经理", company: "智聘科技", score: "86", note: "匹配度高，JD 清晰" },
  { title: "B端产品负责人", company: "星火互娱", score: "82", note: "经验要求匹配" },
  { title: "运营策略专家", company: "云图", score: "79", note: "薪资区间匹配" },
];

const drafts = [
  { company: "智聘科技", role: "资深产品经理", status: "待确认" },
  { company: "星火互娱", role: "B端产品负责人", status: "待确认" },
];

const replies = [
  { company: "云图", intent: "面试邀约", time: "1小时前" },
  { company: "山海数据", intent: "补充材料", time: "3小时前" },
];

export default function Home() {
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
            {recommendations.map((item) => (
              <div className="list-item" key={item.title}>
                <div>
                  <p className="title">{item.title}</p>
                  <p className="muted">{item.company}</p>
                  <p className="hint">{item.note}</p>
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
              <span className="pill">2</span>
            </div>
            <div className="list">
              {drafts.map((item) => (
                <div className="list-item compact" key={item.company}>
                  <div>
                    <p className="title">{item.company}</p>
                    <p className="muted">{item.role}</p>
                  </div>
                  <span className="tag">{item.status}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="card">
            <div className="card-head">
              <h2>待处理回复</h2>
              <span className="pill warning">3</span>
            </div>
            <div className="list">
              {replies.map((item) => (
                <div className="list-item compact" key={item.company}>
                  <div>
                    <p className="title">{item.company}</p>
                    <p className="muted">{item.intent}</p>
                  </div>
                  <span className="tag ghost">{item.time}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}
