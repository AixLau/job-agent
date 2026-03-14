# 求职智能体全量功能设计（MVP + 后续）

**目标**
- 在浏览器插件场景下实现“岗位筛选 + 首轮沟通 + 回复跟进”的完整闭环
- 服务端具备可治理、可审计、可回溯的智能体编排能力
- 支持后续多平台、多轮对话与自动跟进扩展

**范围覆盖**
- PRD 全量功能（MVP + 后续范围）
- 技术选型与架构文档中的全部模块

**非目标**
- 规避平台风控的能力
- 群控/多账号运营能力
- 离线托管全自动投递

---

## 总体架构

三层结构：
1. **浏览器插件（执行层）**：页面识别、数据抽取、UI 注入、草稿填充、聊天读取
2. **服务端业务层（Spring Boot）**：账号、任务、简历、审批、规则、审计、API
3. **智能体编排层（LangGraph Worker）**：岗位分析、消息生成、回复分类、HITL

---

## 核心功能设计

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
- id, taskId, source, externalId, title, company, jdRaw, parsedJson, createdAt

**JobMatch**
- id, taskId, jobPostId, score, reasonJson, riskTagsJson, createdAt

**Conversation**
- id, taskId, jobPostId, externalId, status, createdAt

**MessageDraft**
- id, conversationId, content, sourceType, approved, createdAt

**AuditLog**
- id, userId, actionType, payload, createdAt

---

## 状态机

**任务状态**
INIT → ACTIVE → PAUSED/COMPLETED/FAILED

**岗位状态**
DISCOVERED → ANALYZED → SHORTLISTED → DRAFTED → SENT → REPLIED → ARCHIVED

**会话状态**
NEW → WAITING_USER → SENT → WAITING_HR → NEEDS_REPLY → INTERVIEW → CLOSED

---

## API 设计（关键接口）

- `/api/tasks` 任务管理
- `/api/resume` 简历上传与获取
- `/api/dashboard` 工作台聚合
- `/plugin/page/report` 页面上报
- `/plugin/chat/report` 聊天上报
- `/plugin/action/report` 动作回执
- `/plugin/heartbeat` 插件心跳

---

## LangGraph 编排

- **GoalGraph**：解析求职目标 → 生成策略
- **JobMatchGraph**：JD 抽取 → 过滤/评分 → 风险标记
- **ConversationGraph**：草稿生成 → 回复意图 → 建议动作 → HITL

持久化 checkpoint，支持中断恢复。

---

## 关键非功能要求

- 插件轻量、启动快
- 数据传输安全
- 全链路审计
- 模型输出规则校验

---

## 实现策略

- 以 MVP 功能打通闭环
- 后续功能采用增量扩展，不破坏已有接口
- 所有新增写入路径均记录 AuditLog

