# Dashboard 持久化设计

**目标**
- 将 Dashboard 推荐/草稿/回复从内存改为数据库持久化
- 保持现有 API 与前端协议不变（字段不丢、不新增）
- 支持最新 N 条聚合展示（默认 20，可配置，**每类列表各取 N 条**）

**范围**
- 新增 Dashboard 相关实体与仓储
- 改造 `DashboardStore` 为 JPA 驱动
- 插件上报流程保持不变，只是写入持久化

**非目标**
- 暂不引入完整 PRD 的 `JobPost/JobMatch/Conversation/MessageDraft` 关系模型
- 不做多租户隔离、权限细化、历史回放 UI
- 不改变前端/插件现有接口

---

## 架构与数据流

**写入路径**
- `PluginGatewayController` 处理 `page/report` 与 `chat/report`
- `DashboardStore` 负责将 `RecommendationItem/DraftItem/ReplyItem` 写入数据库

**读取路径**
- `DashboardController` 调用 `DashboardStore.snapshot()`
- `DashboardStore` 从数据库分别取三类最近 N 条，并计算指标

---

## 数据模型

为保持 MVP 简洁，独立三张表：

1. `dashboard_recommendations`
   - `id` (String, PK)
   - `title` (String)
   - `company` (String)
   - `score` (int)
   - `reasons_json` (text, JSON 字符串)
   - `created_at` (Instant)

2. `dashboard_drafts`
   - `id` (String, PK)
   - `company` (String)
   - `title` (String)
   - `content` (text)
   - `created_at` (Instant)

3. `dashboard_replies`
   - `id` (String, PK)
   - `company` (String)
   - `intent` (String)
   - `summary` (text)
   - `next_action` (String)
   - `created_at` (Instant)

**字段映射**
- 当前 DTO 字段集合即为输出字段集合（无额外字段）：\n  `RecommendationItem(title, company, score, reasons)`\n  `DraftItem(company, title, content)`\n  `ReplyItem(company, intent, summary, nextAction)`
- `RecommendationItem` → `dashboard_recommendations`
  - `title` → `title`
  - `company` → `company`
  - `score` → `score`
  - `reasons` → `reasons_json`（JSON 数组字符串）
- `DraftItem` → `dashboard_drafts`
  - `company` → `company`
  - `title` → `title`
  - `content` → `content`
- `ReplyItem` → `dashboard_replies`
  - `company` → `company`
  - `intent` → `intent`
  - `summary` → `summary`
  - `nextAction` → `next_action`

**ID 与时间**
- `id` 使用 UUID 生成（在 `DashboardStore` 内生成）
- `created_at` 使用 `Instant.now()` 写入，`@PrePersist` 兜底
  - 上报 payload 中当前没有可用 ID 字段（DTO 未定义），因此不保留外部 ID

**排序与截断**
- 配置项：`job-agent.dashboard.max-items`，默认 20
- 仓储使用 `Pageable` 按 `created_at` 倒序取 `N` 条
- `DashboardStore.snapshot()` 仅返回最近 N 条

---

## API 兼容性

- `GET /api/dashboard` 返回结构保持不变
- `POST /plugin/page/report`、`POST /plugin/chat/report` 仍返回原响应

---

## 错误处理

- `reasons_json` 解析失败时，回退为空列表
- JSON 序列化失败不阻断主流程：存储空 `[]`
- 数据库写入失败：单条 best-effort，记录日志，不抛出到控制器，主流程返回 `ok`\n  - `page/report` 内推荐与草稿分别尝试写入，任一失败不影响另一条写入
- 数据库读取失败：返回空 `DashboardResponse`（指标为 0）

---

## 测试策略

- 新增仓储层单测：保存并读取推荐/草稿/回复实体
- 新增 `DashboardStore` 单测：
  - `addRecommendation/addDraft/addReply` 后 `snapshot()` 计数正确
  - `snapshot()` 返回按 `created_at` 倒序
  - `metrics.interviews` 统计 `intent=INTERVIEW` 的数量

---

## 迁移与回滚

- 使用 `ddl-auto: update` 自动建表（MVP）
- 若需回滚，可恢复到内存 `DashboardStore`（保留接口）

**指标口径**
- `metrics.recommendations/drafts/replies` 为返回列表长度\n  (即每类最近 N 条的数量，通常等于 N 或当前总量)
- `metrics.interviews` 为返回 replies 列表中 `intent=INTERVIEW` 的数量

**reasons_json 格式**
- JSON 字符串，数组元素为字符串（与 `RecommendationItem.reasons` 一致）
