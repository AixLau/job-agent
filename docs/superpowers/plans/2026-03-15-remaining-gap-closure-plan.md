# Remaining Gap Closure Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补齐当前仓库中尚未落地的核心 PRD 功能缺口，直到剩余清单全部关闭。

**Architecture:** 继续沿用现有 `Spring Boot + Worker(FastAPI) + MV3 Extension + Next.js` 架构，不做大重构。服务端负责结构化任务、规则过滤、审计、状态机与治理；worker 负责更强的解析与生成；Web 补齐管理页与独立工作流；插件补齐 side panel、可见控制与聊天执行体验。

**Tech Stack:** Spring Boot 3.x, JPA, FastAPI, React/Next.js, Chrome Extension MV3, Node test runner, JUnit, unittest.

---

## Completion Rule

本计划中所有任务必须全部完成，任务才允许结束。

允许中途新增或调整计划，但必须满足：
- 新增项写回本计划文档
- 新增项进入对应 `Chunk`
- 只有所有 `Chunk` 下的任务都完成，才允许宣布“全部完成”

---

## File Map

**Server**
- Modify: `server/src/main/java/com/jobagent/server/controller/AuthController.java`
- Modify: `server/src/main/java/com/jobagent/server/controller/AuditController.java`
- Modify: `server/src/main/java/com/jobagent/server/controller/ConversationController.java`
- Modify: `server/src/main/java/com/jobagent/server/controller/DraftController.java`
- Modify: `server/src/main/java/com/jobagent/server/controller/JobActionController.java`
- Modify: `server/src/main/java/com/jobagent/server/controller/PluginGatewayController.java`
- Modify: `server/src/main/java/com/jobagent/server/controller/ResumeController.java`
- Modify: `server/src/main/java/com/jobagent/server/controller/TaskController.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/AuditItem.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/BlacklistCompanyRequest.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/ChatReportResponse.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/DraftItem.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/ReplyItem.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/ResumeRequest.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/TaskCreateRequest.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/TaskUpdateRequest.java`
- Modify: `server/src/main/java/com/jobagent/server/repository/AuditLogRepository.java`
- Modify: `server/src/main/java/com/jobagent/server/repository/JobMatchRepository.java`
- Modify: `server/src/main/java/com/jobagent/server/repository/JobPostRepository.java`
- Modify: `server/src/main/java/com/jobagent/server/repository/MessageDraftRepository.java`
- Modify: `server/src/main/java/com/jobagent/server/repository/UserCompanyBlacklistRepository.java`
- Modify: `server/src/main/java/com/jobagent/server/repository/UserJobActionRepository.java`
- Modify: `server/src/main/java/com/jobagent/server/service/AuditService.java`
- Modify: `server/src/main/java/com/jobagent/server/service/ConversationService.java`
- Modify: `server/src/main/java/com/jobagent/server/service/JobPostService.java`
- Modify: `server/src/main/java/com/jobagent/server/service/ModelOutputValidator.java`
- Modify: `server/src/main/java/com/jobagent/server/service/RuleConfigParser.java`
- Modify: `server/src/main/java/com/jobagent/server/service/RuleEngineService.java`
- Modify: `server/src/main/java/com/jobagent/server/service/StrategyService.java`
- Modify: `server/src/main/java/com/jobagent/server/store/AuditLogEntity.java`
- Modify: `server/src/main/java/com/jobagent/server/store/DashboardStore.java`
- Modify: `server/src/main/java/com/jobagent/server/store/ResumeStore.java`
- Modify: `server/src/main/java/com/jobagent/server/store/TaskEntity.java`
- Modify: `server/src/main/java/com/jobagent/server/store/TaskStore.java`
- Create: `server/src/main/java/com/jobagent/server/controller/ProfileController.java`
- Create: `server/src/main/java/com/jobagent/server/dto/ProfileRequest.java`
- Create: `server/src/main/java/com/jobagent/server/dto/ProfileResponse.java`
- Create: `server/src/main/java/com/jobagent/server/dto/ResumeConfirmRequest.java`
- Create: `server/src/main/java/com/jobagent/server/dto/ResumeParseResponse.java`
- Create: `server/src/main/java/com/jobagent/server/dto/RecommendationListResponse.java`
- Create: `server/src/main/java/com/jobagent/server/dto/ReplyListResponse.java`
- Create: `server/src/main/java/com/jobagent/server/dto/SettingsRequest.java`
- Create: `server/src/main/java/com/jobagent/server/dto/SettingsResponse.java`
- Create: `server/src/main/java/com/jobagent/server/repository/ProfileRepository.java`
- Create: `server/src/main/java/com/jobagent/server/service/FollowUpPolicyService.java`
- Create: `server/src/main/java/com/jobagent/server/service/ProfileService.java`
- Create: `server/src/main/java/com/jobagent/server/service/ResumeParseService.java`
- Create: `server/src/main/java/com/jobagent/server/service/StructuredTaskService.java`
- Create: `server/src/main/java/com/jobagent/server/service/WorkerOutputAuditMapper.java`
- Create: `server/src/main/java/com/jobagent/server/store/ProfileEntity.java`
- Test: `server/src/test/java/com/jobagent/server/ProfileControllerTest.java`
- Test: `server/src/test/java/com/jobagent/server/ResumeControllerTest.java`
- Test: `server/src/test/java/com/jobagent/server/TaskControllerTest.java`
- Test: `server/src/test/java/com/jobagent/server/DashboardControllerTest.java`
- Test: `server/src/test/java/com/jobagent/server/controller/AuditControllerTest.java`
- Test: `server/src/test/java/com/jobagent/server/controller/JobActionControllerTest.java`
- Test: `server/src/test/java/com/jobagent/server/PluginGatewayControllerTest.java`
- Test: `server/src/test/java/com/jobagent/server/service/RuleEngineServiceTest.java`
- Test: `server/src/test/java/com/jobagent/server/service/ConversationServiceTest.java`

**Worker**
- Modify: `worker/src/job_agent_worker/app.py`
- Modify: `worker/src/job_agent_worker/models.py`
- Create: `worker/src/job_agent_worker/task_parser.py`
- Create: `worker/src/job_agent_worker/follow_up.py`
- Create: `worker/src/job_agent_worker/validators.py`
- Test: `worker/tests/test_worker_api.py`
- Test: `worker/tests/test_task_parser.py`
- Test: `worker/tests/test_follow_up.py`

**Web**
- Modify: `web/src/app/page.tsx`
- Modify: `web/src/app/profile/page.tsx`
- Modify: `web/src/app/resume/page.tsx`
- Modify: `web/src/app/tasks/page.tsx`
- Modify: `web/src/lib/dashboard.js`
- Modify: `web/src/lib/resume.js`
- Modify: `web/src/lib/tasks.js`
- Create: `web/src/app/settings/page.tsx`
- Create: `web/src/app/tasks/[id]/page.tsx`
- Create: `web/src/app/recommendations/page.tsx`
- Create: `web/src/app/replies/page.tsx`
- Create: `web/src/app/interviews/page.tsx`
- Create: `web/src/lib/profile.js`
- Create: `web/src/lib/settings.js`
- Create: `web/src/lib/recommendations.js`
- Create: `web/src/lib/replies.js`
- Create: `web/src/lib/interviews.js`
- Test: `web/tests/profile.test.mjs`
- Test: `web/tests/resume.test.mjs`
- Test: `web/tests/tasks.test.mjs`
- Test: `web/tests/recommendations.test.mjs`
- Test: `web/tests/replies.test.mjs`
- Test: `web/tests/interviews.test.mjs`
- Test: `web/tests/settings.test.mjs`

**Extension**
- Modify: `extension/src/api.js`
- Modify: `extension/src/background.js`
- Modify: `extension/src/content.js`
- Modify: `extension/src/popup.html`
- Modify: `extension/src/popup.js`
- Modify: `extension/src/ui.js`
- Modify: `extension/src/styles.css`
- Create: `extension/src/sidepanel.html`
- Create: `extension/src/sidepanel.js`
- Create: `extension/src/sidepanel.css`
- Test: `extension/tests/api.test.mjs`
- Test: `extension/tests/ui.test.mjs`
- Test: `extension/tests/sidepanel.test.mjs`

---

## Chunk 1: Profile + Resume Workflow

### Task 1: Add real profile management

**Files:**
- Create: `server/src/main/java/com/jobagent/server/store/ProfileEntity.java`
- Create: `server/src/main/java/com/jobagent/server/repository/ProfileRepository.java`
- Create: `server/src/main/java/com/jobagent/server/dto/ProfileRequest.java`
- Create: `server/src/main/java/com/jobagent/server/dto/ProfileResponse.java`
- Create: `server/src/main/java/com/jobagent/server/service/ProfileService.java`
- Create: `server/src/main/java/com/jobagent/server/controller/ProfileController.java`
- Modify: `web/src/app/profile/page.tsx`
- Create: `web/src/lib/profile.js`
- Test: `server/src/test/java/com/jobagent/server/ProfileControllerTest.java`
- Test: `web/tests/profile.test.mjs`

- [x] Step 1: 写失败测试，覆盖资料获取、更新、鉴权与字段校验
- [x] Step 2: 运行后端测试，确认失败
- [x] Step 3: 实现 `ProfileEntity/ProfileService/ProfileController`
- [x] Step 4: 运行后端测试，确认通过
- [x] Step 5: 写 Web 失败测试，覆盖页面加载、提交与错误提示
- [x] Step 6: 实现 `profile` 页面与 `web/src/lib/profile.js`
- [x] Step 7: 运行 Web 测试，确认通过
- [x] Step 8: 提交

### Task 2: Add file-based resume upload and parse confirmation

**Files:**
- Modify: `server/src/main/java/com/jobagent/server/dto/ResumeRequest.java`
- Create: `server/src/main/java/com/jobagent/server/dto/ResumeParseResponse.java`
- Create: `server/src/main/java/com/jobagent/server/dto/ResumeConfirmRequest.java`
- Create: `server/src/main/java/com/jobagent/server/service/ResumeParseService.java`
- Modify: `server/src/main/java/com/jobagent/server/store/ResumeStore.java`
- Modify: `server/src/main/java/com/jobagent/server/controller/ResumeController.java`
- Modify: `worker/src/job_agent_worker/models.py`
- Modify: `worker/src/job_agent_worker/app.py`
- Modify: `web/src/app/resume/page.tsx`
- Modify: `web/src/lib/resume.js`
- Test: `server/src/test/java/com/jobagent/server/ResumeControllerTest.java`
- Test: `worker/tests/test_worker_api.py`
- Test: `web/tests/resume.test.mjs`

- [x] Step 1: 写失败测试，覆盖 `TEXT/PDF/DOCX` 元数据、解析预览、确认入库
- [x] Step 2: 运行后端/worker 测试，确认失败
- [x] Step 3: 为后端增加“解析预览 + 确认保存”接口
- [x] Step 4: 为 worker 增加简历解析契约，先做 MVP 解析字段
- [x] Step 5: 运行后端/worker 测试，确认通过
- [x] Step 6: 写 Web 失败测试，覆盖文件选择、解析预览、确认保存
- [x] Step 7: 改造 `resume` 页面支持文件和解析确认
- [x] Step 8: 运行 Web 测试，确认通过
- [x] Step 9: 提交

---

## Chunk 2: Structured Task + Hard Filter

### Task 3: Replace lightweight goal parse with structured task parsing

**Files:**
- Create: `server/src/main/java/com/jobagent/server/service/StructuredTaskService.java`
- Modify: `server/src/main/java/com/jobagent/server/service/StrategyService.java`
- Modify: `server/src/main/java/com/jobagent/server/store/TaskStore.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/TaskCreateRequest.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/TaskUpdateRequest.java`
- Create: `worker/src/job_agent_worker/task_parser.py`
- Modify: `worker/src/job_agent_worker/app.py`
- Modify: `worker/src/job_agent_worker/models.py`
- Modify: `web/src/app/tasks/page.tsx`
- Modify: `web/src/lib/tasks.js`
- Test: `server/src/test/java/com/jobagent/server/TaskControllerTest.java`
- Test: `worker/tests/test_task_parser.py`
- Test: `web/tests/tasks.test.mjs`

- [x] Step 1: 写失败测试，覆盖自然语言解析到 `title/city/salary/experience/exclude/preferences/automation_level`
- [x] Step 2: 运行测试，确认失败
- [x] Step 3: worker 实现 `task_parser`
- [x] Step 4: server 接入结构化结果并保留原始 strategy
- [x] Step 5: 运行后端/worker 测试，确认通过
- [x] Step 6: 写 Web 失败测试，覆盖完整字段表单与自然语言辅助输入
- [x] Step 7: 改造任务页
- [x] Step 8: 运行 Web 测试，确认通过
- [x] Step 9: 提交

### Task 4: Implement real hard filters and recommendation suppression

**Files:**
- Modify: `server/src/main/java/com/jobagent/server/service/RuleConfigParser.java`
- Modify: `server/src/main/java/com/jobagent/server/service/RuleEngineService.java`
- Modify: `server/src/main/java/com/jobagent/server/service/JobPostService.java`
- Modify: `server/src/main/java/com/jobagent/server/repository/JobPostRepository.java`
- Modify: `server/src/main/java/com/jobagent/server/repository/UserCompanyBlacklistRepository.java`
- Modify: `server/src/main/java/com/jobagent/server/repository/UserJobActionRepository.java`
- Modify: `server/src/main/java/com/jobagent/server/controller/JobActionController.java`
- Test: `server/src/test/java/com/jobagent/server/service/RuleEngineServiceTest.java`
- Test: `server/src/test/java/com/jobagent/server/controller/JobActionControllerTest.java`
- Test: `server/src/test/java/com/jobagent/server/DashboardControllerTest.java`

- [x] Step 1: 写失败测试，覆盖城市/薪资/年限/排除项/偏好项过滤
- [x] Step 2: 写失败测试，覆盖黑名单公司拦截
- [x] Step 3: 写失败测试，覆盖 `IGNORE/ARCHIVED` 后不再进入推荐/工作台
- [x] Step 4: 运行测试，确认失败
- [x] Step 5: 在 `RuleEngineService` 落地 `hard_filter_pass`
- [x] Step 6: 在 `JobPostService` 接入黑名单与忽略过滤
- [x] Step 7: 让 dashboard/recommendation 查询只返回可见岗位
- [x] Step 8: 运行测试，确认通过
- [x] Step 9: 提交

---

## Chunk 3: Audit Enrichment + Validation

### Task 5: Enrich audit detail fields

**Files:**
- Modify: `server/src/main/java/com/jobagent/server/store/AuditLogEntity.java`
- Modify: `server/src/main/java/com/jobagent/server/service/AuditService.java`
- Create: `server/src/main/java/com/jobagent/server/service/WorkerOutputAuditMapper.java`
- Modify: `server/src/main/java/com/jobagent/server/controller/AuditController.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/AuditItem.java`
- Modify: `web/src/app/audits/page.tsx`
- Modify: `web/src/lib/audits.js`
- Test: `server/src/test/java/com/jobagent/server/controller/AuditControllerTest.java`
- Test: `web/tests/audits.test.mjs`

- [x] Step 1: 写失败测试，覆盖 `result/model_output/risk_tags` 输出
- [x] Step 2: 运行测试，确认失败
- [x] Step 3: 扩展审计存储结构与记录入口
- [x] Step 4: 让审计页展示详细字段
- [x] Step 5: 运行后端/Web 测试，确认通过
- [x] Step 6: 提交

### Task 6: Expand model output validation coverage

**Files:**
- Modify: `server/src/main/java/com/jobagent/server/service/ModelOutputValidator.java`
- Modify: `server/src/main/java/com/jobagent/server/service/JobPostService.java`
- Modify: `server/src/main/java/com/jobagent/server/service/ConversationService.java`
- Create: `worker/src/job_agent_worker/validators.py`
- Modify: `worker/src/job_agent_worker/app.py`
- Test: `server/src/test/java/com/jobagent/server/service/ConversationServiceTest.java`
- Test: `server/src/test/java/com/jobagent/server/service/RuleEngineServiceTest.java`
- Test: `worker/tests/test_worker_api.py`

- [x] Step 1: 写失败测试，覆盖任务解析、岗位解析、回复分类、跟进建议、面试草稿的校验
- [x] Step 2: 运行测试，确认失败
- [x] Step 3: 补齐服务端和 worker 校验器
- [x] Step 4: 运行测试，确认通过
- [x] Step 5: 提交

---

## Chunk 4: Conversation + Interview + Follow-up

### Task 7: Build interview invitation workflow

**Files:**
- Modify: `worker/src/job_agent_worker/app.py`
- Modify: `server/src/main/java/com/jobagent/server/service/ConversationService.java`
- Modify: `server/src/main/java/com/jobagent/server/store/DashboardStore.java`
- Modify: `server/src/main/java/com/jobagent/server/controller/ConversationController.java`
- Modify: `web/src/app/page.tsx`
- Create: `web/src/app/interviews/page.tsx`
- Create: `web/src/lib/interviews.js`
- Test: `server/src/test/java/com/jobagent/server/PluginGatewayControllerTest.java`
- Test: `web/tests/interviews.test.mjs`

- [x] Step 1: 写失败测试，覆盖面试邀约识别、草稿生成、工作台展示、独立页面展示
- [x] Step 2: 运行测试，确认失败
- [x] Step 3: 打通 worker → server → dashboard → web 页面链路
- [x] Step 4: 运行测试，确认通过
- [x] Step 5: 提交

### Task 8: Add multi-turn reply strategy and auto follow-up suggestion

**Files:**
- Create: `server/src/main/java/com/jobagent/server/service/FollowUpPolicyService.java`
- Modify: `server/src/main/java/com/jobagent/server/service/ConversationService.java`
- Create: `worker/src/job_agent_worker/follow_up.py`
- Modify: `worker/src/job_agent_worker/app.py`
- Create: `web/src/app/replies/page.tsx`
- Create: `web/src/lib/replies.js`
- Test: `server/src/test/java/com/jobagent/server/service/ConversationServiceTest.java`
- Test: `worker/tests/test_follow_up.py`
- Test: `web/tests/replies.test.mjs`

- [x] Step 1: 写失败测试，覆盖多轮对话状态推进与自动跟进建议
- [x] Step 2: 运行测试，确认失败
- [x] Step 3: 实现 worker 跟进建议生成
- [x] Step 4: 实现 server 状态机与 reply list API
- [x] Step 5: 实现 Web 待处理回复页
- [x] Step 6: 运行测试，确认通过
- [x] Step 7: 提交

### Task 9: Add minimal multi-agent orchestration hooks

**Files:**
- Modify: `server/src/main/java/com/jobagent/server/service/StrategyService.java`
- Modify: `server/src/main/java/com/jobagent/server/service/JobPostService.java`
- Modify: `server/src/main/java/com/jobagent/server/service/ConversationService.java`
- Modify: `worker/src/job_agent_worker/app.py`
- Modify: `worker/src/job_agent_worker/models.py`
- Test: `server/src/test/java/com/jobagent/server/service/ConversationServiceTest.java`
- Test: `worker/tests/test_worker_api.py`

- [x] Step 1: 写失败测试，覆盖“解析/匹配/对话建议”并行子任务契约
- [x] Step 2: 运行测试，确认失败
- [x] Step 3: 用当前架构实现最小并行编排接口，不引入大重构
- [x] Step 4: 运行测试，确认通过
- [x] Step 5: 提交

---

## Chunk 5: Web Workbench Completion

### Task 10: Add standalone task detail, recommendation, settings pages

**Files:**
- Create: `web/src/app/tasks/[id]/page.tsx`
- Create: `web/src/app/recommendations/page.tsx`
- Create: `web/src/app/settings/page.tsx`
- Create: `web/src/lib/recommendations.js`
- Create: `web/src/lib/settings.js`
- Modify: `web/src/app/page.tsx`
- Modify: `web/src/lib/dashboard.js`
- Test: `web/tests/recommendations.test.mjs`
- Test: `web/tests/settings.test.mjs`

- [x] Step 1: 写失败测试，覆盖页面数据加载、空态与错误态
- [x] Step 2: 运行测试，确认失败
- [x] Step 3: 实现推荐岗位页、任务详情页、设置页
- [x] Step 4: 把首页跳转入口补齐
- [x] Step 5: 运行测试，确认通过
- [x] Step 6: 提交

### Task 11: Improve recommendation explainability

**Files:**
- Modify: `server/src/main/java/com/jobagent/server/service/JobPostService.java`
- Modify: `server/src/main/java/com/jobagent/server/store/DashboardStore.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/DraftItem.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/ReplyItem.java`
- Modify: `web/src/lib/dashboard.js`
- Modify: `web/src/app/page.tsx`
- Create: `web/tests/recommendations.test.mjs`
- Test: `server/src/test/java/com/jobagent/server/DashboardControllerTest.java`

- [x] Step 1: 写失败测试，覆盖推荐理由、过滤原因、风险提示完整展示
- [x] Step 2: 运行测试，确认失败
- [x] Step 3: 补齐 server DTO 与 dashboard 数据
- [x] Step 4: 补齐首页和推荐页的解释性展示
- [x] Step 5: 运行测试，确认通过
- [x] Step 6: 提交

---

## Chunk 6: Extension UX Completion

### Task 12: Add side panel and explicit task status panel

**Files:**
- Create: `extension/src/sidepanel.html`
- Create: `extension/src/sidepanel.js`
- Create: `extension/src/sidepanel.css`
- Modify: `extension/src/background.js`
- Modify: `extension/src/popup.html`
- Modify: `extension/src/popup.js`
- Test: `extension/tests/sidepanel.test.mjs`

- [x] Step 1: 写失败测试，覆盖 side panel 状态同步与任务状态展示
- [x] Step 2: 运行测试，确认失败
- [x] Step 3: 实现 side panel 与后台同步
- [x] Step 4: 运行测试，确认通过
- [x] Step 5: 提交

### Task 13: Componentize draft confirmation and automation control

**Files:**
- Modify: `extension/src/ui.js`
- Modify: `extension/src/content.js`
- Modify: `extension/src/styles.css`
- Modify: `extension/src/api.js`
- Test: `extension/tests/ui.test.mjs`
- Test: `extension/tests/api.test.mjs`

- [x] Step 1: 写失败测试，覆盖独立草稿确认框、风险说明、终止按钮、解释说明
- [x] Step 2: 运行测试，确认失败
- [x] Step 3: 重构 overlay 为独立确认组件
- [x] Step 4: 补齐自动化动作“可见、可解释、可终止”
- [x] Step 5: 运行测试，确认通过
- [x] Step 6: 提交

### Task 14: Extend chat execution for interview and follow-up

**Files:**
- Modify: `extension/src/background.js`
- Modify: `extension/src/content.js`
- Modify: `extension/src/ui.js`
- Test: `extension/tests/ui.test.mjs`
- Test: `extension/tests/api.test.mjs`

- [x] Step 1: 写失败测试，覆盖面试邀约草稿、自动跟进草稿、暂停/恢复行为
- [x] Step 2: 运行测试，确认失败
- [x] Step 3: 扩展聊天页执行逻辑
- [x] Step 4: 运行测试，确认通过
- [x] Step 5: 提交

---

## Chunk 7: Final Verification

### Task 15: Full regression and gap checklist closeout

**Files:**
- Verify only: `server/**`
- Verify only: `worker/**`
- Verify only: `web/**`
- Verify only: `extension/**`
- Modify if needed: `docs/superpowers/plans/2026-03-15-remaining-gap-closure-plan.md`

回归结果：
- 无新增产品缺口
- 修复了一处测试隔离问题：`DashboardControllerTest` 未清理 `message_drafts`，导致全量回归时面试草稿断言被脏数据污染

- [x] Step 1: 逐条对照本计划与原始缺口清单，记录剩余问题
- [x] Step 2: 运行 `cd server && mvn -Dmaven.repo.local=/Users/lushiwu/dev/apache-maven-3.9.4/rep test`
- [x] Step 3: 运行 `PYTHONPATH=worker/src python3 -m unittest worker/tests/test_worker_api.py worker/tests/test_task_parser.py worker/tests/test_follow_up.py`
- [x] Step 4: 运行 `cd web && node --test tests/*.mjs`
- [x] Step 5: 运行 `cd extension && node --test tests/*.mjs`
- [x] Step 6: 若有新增缺口，写回本计划并补做实现
- [x] Step 7: 所有缺口关闭后再允许结束任务

---

## Execution Order

必须按以下顺序执行：
1. `Chunk 1`
2. `Chunk 2`
3. `Chunk 3`
4. `Chunk 4`
5. `Chunk 5`
6. `Chunk 6`
7. `Chunk 7`

其中：
- `Chunk 2` 完成前，不允许宣称“推荐链路闭环”
- `Chunk 3` 完成前，不允许宣称“审计与治理完整”
- `Chunk 4` 完成前，不允许宣称“对话智能体完整”
- `Chunk 5` 完成前，不允许宣称“Web MVP 补齐”
- `Chunk 6` 完成前，不允许宣称“插件端补齐”
- `Chunk 7` 完成前，不允许宣称“全部完成”
