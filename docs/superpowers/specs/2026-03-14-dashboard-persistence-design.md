# Dashboard 持久化设计

**目标**
- 将 Dashboard 推荐/草稿/回复从内存改为数据库持久化
- 保持现有 API 与前端协议不变
- 支持最新 N 条聚合展示（默认 20）

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
- `DashboardStore` 从数据库取最近 N 条，并计算指标

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

**排序与截断**
- 仓储提供 `findTop20ByOrderByCreatedAtDesc()`
- `DashboardStore` 只返回最近 N 条

---

## API 兼容性

- `GET /api/dashboard` 返回结构保持不变
- `POST /plugin/page/report`、`POST /plugin/chat/report` 仍返回原响应

---

## 错误处理

- `reasons_json` 解析失败时，回退为空列表
- 任何 JSON 序列化失败不阻断主流程：存储空 `[]`

---

## 测试策略

- 新增仓储层单测：保存并读取推荐/草稿/回复实体
- 新增 `DashboardStore` 单测：
  - `addRecommendation/addDraft/addReply` 后 `snapshot()` 计数正确
  - `snapshot()` 返回按 `created_at` 倒序

---

## 迁移与回滚

- 使用 `ddl-auto: update` 自动建表（MVP）
- 若需回滚，可恢复到内存 `DashboardStore`（保留接口）
