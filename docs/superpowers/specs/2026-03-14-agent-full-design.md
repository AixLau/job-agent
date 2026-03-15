# 求职智能体全量功能设计（MVP 为主，后续扩展附录）

**目标**
- 在浏览器插件场景下实现“岗位筛选 + 首轮沟通 + 回复跟进”的完整闭环
- 服务端具备可治理、可审计、可回溯的智能体编排能力
- 支持后续多平台、多轮对话与自动跟进扩展

**范围覆盖**
- MVP 全功能闭环（插件 + 服务端 + LangGraph）
- 后续扩展作为附录描述（不进入本次实施计划）

**非目标**
- 规避平台风控的能力
- 群控/多账号运营能力
- 离线托管全自动投递

---

## MVP 目标平台与页面类型

**平台**
- MVP 仅支持 Boss 直聘网页端（`zhipin.com`）

**页面类型**
- 职位列表页
- 职位详情页
- 聊天页（与 HR 对话）

**抽取字段（最小）**
- Job: `title`, `company`, `salary`, `experience`, `city`, `jd_raw`
- Chat: `conversation_id`, `messages[{id,role,text,ts}]`

---

## MVP 范围与边界

**MVP In-Scope**
1. 插件：页面识别、JD 抽取、UI 注入、聊天读取、草稿填充、心跳
2. 服务端：账号/简历/任务/审批/审计/规则校验
3. Agent：岗位分析、草稿生成、回复分类、HITL 节点
4. 工作台：推荐/草稿/待处理回复/面试提醒

**MVP Out-of-Scope**
- 多平台支持
- 自动跟进策略
- 数据复盘与转化分析
- 群控/自动投递

**范围说明**
- 本次为单一 MVP 交付物，包含插件、服务端、Worker 与工作台
- 实施将按阶段推进，但不拆分为独立项目

**实施阶段（计划视角）**
1. 插件上报 + 服务端入库 + 基础审计闭环
2. Worker 集成（评分/草稿/回复分类）+ 规则校验
3. 工作台聚合与操作接口（审批/关闭/重生成）

---

## 总体架构

三层结构：
1. **浏览器插件（执行层）**：页面识别、数据抽取、UI 注入、草稿填充、聊天读取
2. **服务端业务层（Spring Boot）**：账号、任务、简历、审批、规则、审计、API
3. **智能体编排层（LangGraph Worker）**：岗位分析、消息生成、回复分类、HITL

---

## 核心功能设计（MVP）

### 1. 账号与个人资料
- 注册/登录
- 简历上传/解析/确认
- 求职偏好保存

### 2. 求职任务
- 创建、编辑、暂停、结束
- 任务状态维护（INIT/ACTIVE/PAUSED/COMPLETED/FAILED）
- 自动化等级（保守/半自动/自动）

### 3. 浏览器插件
- 页面识别（列表/详情/聊天）
- JD 抽取与上报
- UI 注入（匹配分、理由、风险、草稿）
- 聊天内容读取与上报
- 一键停用/终止

### 4. 岗位分析
- JD 结构化解析
- 硬条件过滤
- 匹配评分
- 风险标签

### 5. 沟通辅助
- 首轮消息草稿
- 回复摘要与意图分类
- 面试邀约草稿与提醒

### 6. 工作台
- 推荐岗位列表
- 待发送草稿列表
- 待处理回复列表
- 面试机会列表
- 历史记录与审计

### 7. 自动化等级
- 保守：只分析推荐
- 半自动：草稿生成，人工确认
- 自动：低风险自动生成草稿，高风险 HITL；所有发送动作仍需用户确认

---

## 插件鉴权

**认证接口**
- `POST /api/auth/register` 账号注册
- `POST /api/auth/login` 账号登录
- `POST /api/auth/plugin/token` 签发插件 token
- `POST /api/auth/plugin/refresh` 刷新插件 token
- `POST /api/auth/plugin/revoke` 撤销插件 token

**Token 规则**
- `plugin_token` 绑定用户与浏览器实例
- 有效期 24h
- 支持刷新与撤销

**认证请求/响应（最小）**
- `RegisterRequest`: `{account, password, email?}`
- `RegisterResponse`: `{user{id,account}}`
- `LoginRequest`: `{account, password}`
- `LoginResponse`: `{access_token, refresh_token, expires_in}`
- `PluginTokenRequest`: `{access_token, browser_id}`
- `PluginTokenResponse`: `{plugin_token, expires_in}`
- `PluginRefreshRequest`: `{plugin_token}`
- `PluginRefreshResponse`: `{plugin_token, expires_in}`
- `PluginRevokeRequest`: `{plugin_token}`
- `PluginRevokeResponse`: `{status}`

---

## 数据模型（核心实体）

**User**
- id, account, profileStatus

**Resume**
- id, userId, content, parsedJson, createdAt

**JobTask**
- id, userId, strategyJson, automationLevel, status, createdAt

**JobPost**
- id, taskId, source, externalId, title, company, salary, experience, city, jdRaw, parsedJson, createdAt, status

**JobMatch**
- id, taskId, jobPostId, score, reasonJson, riskTagsJson, createdAt

**Conversation**
- id, taskId, jobPostId, externalId, status, createdAt

**Message**
- id, conversationId, role, content, externalId, createdAt

**MessageDraft**
- id, conversationId, content, sourceType, approved, createdAt

**AuditLog**
- id, userId, actionType, payload, createdAt

---

## 外部 ID 映射与唯一性

**平台外部 ID 约定（Boss 直聘）**
- `JobPost.externalId` = 职位详情页的唯一岗位 ID（从 URL 或页面 data 属性解析）
- `Conversation.externalId` = 聊天会话 ID（插件上报 `conversation_id`）
- `Message.externalId` = 聊天消息 ID（插件上报 `messages[].id`）

**唯一性约束（MVP）**
- JobPost 唯一：`source + external_id`
- Conversation 唯一：`task_id + conversation_external_id`
- Message 唯一：`conversation_id + message_external_id`

---

## 状态机与触发

**任务状态**
- INIT → ACTIVE（创建任务）
- ACTIVE → PAUSED（用户暂停）
- ACTIVE → COMPLETED（用户结束）
- ACTIVE → FAILED（系统异常）

**岗位状态**
- DISCOVERED（页面发现）
- ANALYZED（JobMatch 评分完成）
- SHORTLISTED（匹配分达阈值）
- DRAFTED（首轮草稿生成）
- SENT（用户确认发送）
- REPLIED（收到 HR 回复）
- INTERVIEW（进入面试）
- ARCHIVED（用户忽略/不合适）

**会话状态**
- NEW（首次建立）
- WAITING_USER（草稿待确认）
- SENT（已发送）
- WAITING_HR（等待回复）
- NEEDS_REPLY（需回复）
- INTERVIEW（进入面试）
- CLOSED（结束）

**岗位状态流转（允许的状态迁移）**
- DISCOVERED → ANALYZED（完成评分）
- ANALYZED → SHORTLISTED（分数达阈值）
- ANALYZED → ARCHIVED（分数未达阈值或用户忽略）
- SHORTLISTED → DRAFTED（草稿生成）
- DRAFTED → SENT（用户确认发送）
- SENT → REPLIED（收到回复）
- REPLIED → INTERVIEW（识别到面试意图）
- REPLIED → CLOSED（明确拒绝/结束）
- ANY → ARCHIVED（用户手动忽略/关闭）

**会话状态流转（允许的状态迁移）**
- NEW → WAITING_USER（草稿生成）
- WAITING_USER → SENT（用户确认发送）
- SENT → WAITING_HR（发送成功）
- WAITING_HR → NEEDS_REPLY（收到回复且需要处理）
- NEEDS_REPLY → WAITING_USER（生成回复草稿）
- NEEDS_REPLY → INTERVIEW（识别为面试邀约）
- INTERVIEW → CLOSED（面试结束或关闭）
- ANY → CLOSED（用户手动关闭）

**触发与责任方（摘要）**
- 页面上报 → JobPost/JobMatch 写入（插件 + 服务端）
- 草稿生成 → JobPost.DRAFTED / Conversation.WAITING_USER（服务端 + Worker）
- 用户点击发送 → JobPost.SENT / Conversation.SENT（插件）
- 聊天上报 → Conversation.NEEDS_REPLY/INTERVIEW（插件 + Worker）

**状态映射**
- Conversation 状态 = NEEDS_REPLY → JobPost 状态 = REPLIED
- Conversation 状态 = INTERVIEW → JobPost 状态 = INTERVIEW
- Conversation 状态 = CLOSED → JobPost 状态 = ARCHIVED

---

## API 设计（关键接口）

**任务/简历**
- `POST /api/tasks` 创建任务（需登录）
- `GET /api/tasks` 任务列表（需登录）
- `POST /api/resume` 上传简历（需登录）
- `GET /api/resume` 获取最新简历（需登录）
- `PATCH /api/tasks/{task_id}` 更新任务（需登录）

**工作台**
- `GET /api/dashboard` 获取聚合快照（需登录）
- `GET /api/conversations/{id}` 获取会话详情（需登录）
- `POST /api/drafts/{id}/approve` 审批草稿（需登录）
- `POST /api/drafts/{id}/reject` 驳回草稿（需登录）
- `POST /api/conversations/{id}/close` 关闭会话（需登录）
- `POST /api/conversations/{id}/regenerate` 重新生成草稿（需登录）

**插件上报**
- `POST /plugin/page/report`（插件 token）
  请求：`PageReportRequest`
  响应：`PageReportResponse`
- `POST /plugin/chat/report`（插件 token）
  请求：`ChatReportRequest`
  响应：`ChatReportResponse`
- `POST /plugin/action/report`（插件 token）
  请求：`ActionReportRequest`
  响应：`StatusResponse`
- `POST /plugin/heartbeat`（插件 token）
  请求：`HeartbeatRequest`
  响应：`StatusResponse`

**请求 Schema（最小）**
- `CreateTaskRequest`:
  - `title` (string)
  - `city` (string)
  - `salary` (string)
  - `experience` (string)
  - `exclude` (array[string])
  - `preferences` (array[string])
  - `automation_level` (string: CONSERVATIVE/SEMI/AUTO)
  - `strategy_text` (string, 用户自然语言输入)
- `CreateTask 处理规则`:
  - 服务端调用 Worker `/worker/goal-parse`（`stage=GOAL_PARSE`）将 `strategy_text` 转为 `strategy_json`
  - `strategy_json` 持久化到 JobTask，用于 JobMatchGraph
- `UpdateTaskRequest`:
  - 同 `CreateTaskRequest`，全部可选
  - 若 `strategy_text` 变化，触发 Worker `/worker/goal-parse` 生成 `strategy_json`
- `ResumeUploadRequest`:
  - `content` (string, 原文/抽取文本)
  - `format` (string: PDF/TEXT)
  - `source` (string, optional)
- `PageReportRequest`:
  - `task_id` (string)
  - `page_type` (list/detail)
  - `raw_text` (string)
  - `extracted_json` (object)
  - `source_url` (string)
  - `dom_hash` (string)
  - `want_draft` (boolean, 仅 detail 页有效)
- `ChatReportRequest`:
  - `task_id` (string)
  - `conversation_id` (string, 对应 Conversation.externalId)
  - `messages` (array[{id,role,text,ts}])
  - `last_message_id` (string)
- `ActionReportRequest`:
  - `task_id` (string)
  - `action_type` (string)
  - `status` (string)
  - `payload` (object)
- `HeartbeatRequest`:
  - `user_id` (string)
  - `task_id` (string)
  - `tab_id` (string)
  - `status` (string)
  - `ts` (number)

**响应 Schema（最小）**
- `CreateTaskResponse`:
  - `task{id, strategy_json, status, automation_level, created_at}`
- `TaskListResponse`:
  - `tasks[]` (包含 `strategy_json`)
- `ResumeResponse`:
  - `resume{id, parsed_json, created_at}`
- `PageReportResponse`: `status`, `analysis{score,reasons,risks}`, `draft?{company,title,content}`
- `ChatReportResponse`: `status`, `reply{intent,summary,next_action}`
- `StatusResponse`: `status`

**工作台响应 Schema（最小）**
- `DashboardResponse`:
  - `metrics{recommendations,drafts,replies,interviews}`
  - `recommendations[]`
  - `drafts[]`
  - `replies[]`
  - `interviews[]`
  - `updated_at`

**实体响应字段（最小）**
- `recommendations[]`: `{job_post_id,title,company,score,risks,status}`
- `drafts[]`: `{draft_id,conversation_id,content,created_at,approved}`
- `replies[]`: `{conversation_id,summary,intent,updated_at}`
- `interviews[]`: `{conversation_id,company,title,scheduled_at?}`

**工作台操作请求（最小）**
- `POST /api/drafts/{id}/approve`:
  - req: `{action: "approve"}`
  - resp: `{status, draft{approved}, action_hint{fill_content}}`
- `POST /api/drafts/{id}/reject`:
  - req: `{reason?}`
  - resp: `{status}`
- `POST /api/conversations/{id}/close`:
  - req: `{reason?}`
  - resp: `{status, conversation{status:"CLOSED"}}`
- `POST /api/conversations/{id}/regenerate`:
  - req: `{style?}`
  - resp: `{status, draft{content}}`

**通用错误响应（非插件 API）**
- 响应体：`error{code,message}`
- 401 `UNAUTHORIZED`
- 403 `FORBIDDEN`
- 404 `NOT_FOUND`
- 400 `VALIDATION_FAILED`
- 429 `RATE_LIMITED`
- 500 `SERVER_ERROR`

---

## LangGraph 编排（MVP）

**GoalGraph**
- 输入：任务策略（文本）
- 输出：结构化策略 JSON

**JobMatchGraph**
- 输入：JobPost + Resume + Strategy
- 输出：Match score + reasons + risks

**ConversationGraph**
- 输入：Conversation + JobPost + Resume + Messages
- 输出：MessageDraft + ReplyIntent + NextAction

**Worker API（最小）**
- `POST /worker/goal-parse`
  - req: `{task_id, stage, strategy_text, idempotency_key}`
  - resp: `{strategy_json}`
- `POST /worker/job-match`
  - req: `{task_id, stage, job_post, resume, strategy, idempotency_key}`
  - resp: `{score, reasons[], risks[]}`
- `POST /worker/draft`
  - req: `{task_id, stage, conversation, job_post, resume, idempotency_key}`
  - resp: `{content}`
- `POST /worker/reply-classify`
  - req: `{task_id, stage, conversation, messages, last_message_id, idempotency_key}`
  - resp: `{intent, summary, next_action}`

**服务端与 Worker 集成契约**
- 调用方式：同步 HTTP（MVP）
- 超时：10s
- 重试：2 次
- 幂等：基于 `idempotency_key`
- Worker 鉴权：内部网络 + `X-Worker-Token` 共享密钥（MVP）

**插件上报同步性（MVP）**
- `page/report` 与 `chat/report` 为同步调用，服务端在请求内调用 Worker
- Worker 超时返回 504，插件提示用户稍后重试
- `page/report` 仅在 `page_type=detail` 且 `want_draft=true` 时返回 `draft`

**Stage 枚举**
- `GOAL_PARSE`
- `JOB_MATCH`
- `DRAFT`
- `REPLY_CLASSIFY`

**idempotency_key 生成规则（MVP）**
- GOAL_PARSE: `task_id + stage + strategy_text_hash`
- JOB_MATCH: `task_id + stage + job_post.external_id + job_post.source`
- DRAFT: `task_id + stage + conversation.external_id + job_post.external_id`
- REPLY_CLASSIFY: `task_id + stage + conversation.external_id + last_message_id`

**strategy_text_hash 规则**
- `strategy_text` 去空白与大小写归一后取 SHA-256

---

## 自动化等级与风控

**低风险**
- 匹配分 >= 70 且无风险标签

**高风险**
- 风险标签命中（outsourcing/overtime/grey）或匹配分 < 70

**HITL**
- 高风险节点必须人工确认
- 所有发送动作仍需用户确认

---

## 去重与幂等策略

- JobPost 去重：`source + external_id`
- Conversation 去重：`task_id + conversation_external_id`
- MessageDraft 去重：`conversation_id + source_type`

---

## 关键非功能要求（MVP）

1. 插件轻量、启动快（MV3）
2. 数据传输安全（HTTPS + token）
3. 全链路审计（AuditLog）
4. 模型输出规则校验（敏感词/格式/长度）
5. 数据保留策略：默认 90 天

**模型输出规则校验（MVP 规则）**
- 长度：首轮草稿 `10-500` 字符；回复摘要 `<= 200` 字符
- 禁止联系方式：手机号、邮箱、微信号、QQ 号
- 禁止敏感词：涉黄/涉政/辱骂（基于可配置词表）
- 禁止夸张承诺：如“保证录用/100%录取”
- 执行点：Worker 输出后进入服务端规则层，校验失败返回 `VALIDATION_FAILED`

**插件错误处理（MVP）**
- 401 `PLUGIN_TOKEN_INVALID`: 需重新登录插件
- 400 `VALIDATION_FAILED`: 显示校验原因，允许用户手动修改
- 409 `DUPLICATE_IGNORED`: 已存在记录，响应体返回已有分析/草稿
- 504 `WORKER_TIMEOUT`: 提示稍后重试

---

## 实现策略

1. 以 MVP 功能打通闭环
2. 后续功能增量扩展，不破坏已有接口
3. 所有写入路径记录 AuditLog

---

## 附录：后续扩展
1. 多平台支持
2. 自动跟进策略
3. 数据复盘与转化分析
4. 多轮对话风格优化
