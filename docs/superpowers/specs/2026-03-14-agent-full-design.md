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
- 自动：低风险自动填充，高风险节点 HITL

---

## 数据模型（核心实体）

**User**
- id, account, profileStatus

**Resume**
- id, userId, content, parsedJson, createdAt

**JobTask**
- id, userId, strategyJson, automationLevel, status, createdAt

**JobPost**
- id, taskId, source, externalId, title, company, jdRaw, parsedJson, createdAt, status

**JobMatch**
- id, taskId, jobPostId, score, reasonJson, riskTagsJson, createdAt

**Conversation**
- id, taskId, jobPostId, externalId, status, createdAt

**MessageDraft**
- id, conversationId, content, sourceType, approved, createdAt

**AuditLog**
- id, userId, actionType, payload, createdAt

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
- ARCHIVED（用户忽略/不合适）

**会话状态**
- NEW（首次建立）
- WAITING_USER（草稿待确认）
- SENT（已发送）
- WAITING_HR（等待回复）
- NEEDS_REPLY（需回复）
- INTERVIEW（进入面试）
- CLOSED（结束）

---

## API 设计（关键接口）

**任务/简历**
- `POST /api/tasks` 创建任务（需登录）
- `GET /api/tasks` 任务列表（需登录）
- `POST /api/resume` 上传简历（需登录）
- `GET /api/resume` 获取最新简历（需登录）

**工作台**
- `GET /api/dashboard` 获取聚合快照（需登录）

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

**错误处理（统一）**
- 4xx：参数缺失/鉴权失败
- 5xx：系统错误（记录审计）
- 插件侧失败不阻断主流程，返回可解释错误码

---

## LangGraph 编排（MVP）

**GoalGraph**
- 输入：任务策略（文本）
- 输出：结构化策略 JSON

**JobMatchGraph**
- 输入：JobPost + Resume + Strategy
- 输出：Match score + reasons + risks

**ConversationGraph**
- 输入：Conversation + JobPost + Resume
- 输出：MessageDraft + ReplyIntent + NextAction

**Checkpoint**
- 每个节点保存 state，支持中断恢复
- 失败策略：节点级 retry（最多 2 次），失败后转 HITL

---

## 关键非功能要求（MVP）

1. 插件轻量、启动快（MV3）
2. 数据传输安全（HTTPS + token）
3. 全链路审计（AuditLog）
4. 模型输出规则校验（敏感词/格式/长度）
5. 数据保留策略：默认 90 天

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
