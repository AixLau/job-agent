# Gap-Fill (Rule Engine) Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补齐全部缺口需求，引入规则引擎治理（硬条件过滤/风险/自动化），完善插件抽取、自动发送、工作台关注/审计与数据保留。

**Architecture:** 在现有 Spring Boot + Worker + 插件 + Web 架构上增加规则引擎服务与偏好/审计/保留模块。规则引擎负责 hard filter、risk tags、automation action；Worker 提供候选解析与标签；插件执行自动发送与状态上报；工作台新增关注与审计页面。

**Tech Stack:** Spring Boot 3.x, JPA(H2/PostgreSQL), FastAPI, MV3, Next.js, Node test runner.

---

## File Map (Planned Changes)

**Server (Spring Boot)**
- Modify: `server/src/main/java/com/jobagent/server/store/TaskEntity.java`
- Modify: `server/src/main/java/com/jobagent/server/store/JobMatchEntity.java`
- Modify: `server/src/main/java/com/jobagent/server/store/TaskStore.java`
- Modify: `server/src/main/java/com/jobagent/server/service/JobPostService.java`
- Modify: `server/src/main/java/com/jobagent/server/service/ConversationService.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/TaskUpdateRequest.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/DraftItem.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/ReplyItem.java`
- Modify: `server/src/main/java/com/jobagent/server/store/DashboardStore.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/WorkerJobMatchResponse.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/WorkerReplyClassifyResponse.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/ChatReportResponse.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/PageReportRequest.java`
- Create: `server/src/main/java/com/jobagent/server/service/RuleEngineService.java`
- Create: `server/src/main/java/com/jobagent/server/service/RuleConfigParser.java`
- Create: `server/src/main/java/com/jobagent/server/service/SalaryParser.java`
- Create: `server/src/main/java/com/jobagent/server/service/ExperienceParser.java`
- Create: `server/src/main/java/com/jobagent/server/service/RiskRuleSet.java`
- Create: `server/src/main/java/com/jobagent/server/service/AutomationPolicy.java`
- Create: `server/src/main/java/com/jobagent/server/service/RuleResult.java`
- Create: `server/src/main/java/com/jobagent/server/store/UserCompanyBlacklistEntity.java`
- Create: `server/src/main/java/com/jobagent/server/store/UserJobActionEntity.java`
- Create: `server/src/main/java/com/jobagent/server/repository/UserCompanyBlacklistRepository.java`
- Create: `server/src/main/java/com/jobagent/server/repository/UserJobActionRepository.java`
- Create: `server/src/main/java/com/jobagent/server/controller/JobActionController.java`
- Create: `server/src/main/java/com/jobagent/server/controller/AuditController.java`
- Create: `server/src/main/java/com/jobagent/server/dto/FollowItem.java`
- Create: `server/src/main/java/com/jobagent/server/dto/FollowListResponse.java`
- Create: `server/src/main/java/com/jobagent/server/dto/BlacklistCompanyRequest.java`
- Create: `server/src/main/java/com/jobagent/server/dto/AuditItem.java`
- Create: `server/src/main/java/com/jobagent/server/dto/AuditListResponse.java`
- Create: `server/src/main/java/com/jobagent/server/service/RetentionService.java`
- Modify: `server/src/main/java/com/jobagent/server/JobAgentServerApplication.java`
- Test: `server/src/test/java/com/jobagent/server/service/RuleEngineServiceTest.java`
- Test: `server/src/test/java/com/jobagent/server/controller/JobActionControllerTest.java`
- Test: `server/src/test/java/com/jobagent/server/controller/AuditControllerTest.java`
- Test: `server/src/test/java/com/jobagent/server/store/TaskStoreTest.java`
- Test: `server/src/test/java/com/jobagent/server/TaskControllerTest.java`
- Test: `server/src/test/java/com/jobagent/server/DashboardControllerTest.java`
- Test: `server/src/test/java/com/jobagent/server/service/RetentionServiceTest.java`

**Worker (FastAPI)**
- Modify: `worker/src/job_agent_worker/models.py`
- Modify: `worker/src/job_agent_worker/app.py`
- Modify: `worker/tests/test_worker_api.py`

**Extension (MV3)**
- Modify: `extension/src/extractor.js`
- Modify: `extension/src/content.js`
- Modify: `extension/src/background.js`
- Modify: `extension/src/api.js`
- Modify: `extension/src/popup.html`
- Modify: `extension/src/popup.js`
- Modify: `extension/src/ui.js`
- Test: `extension/tests/extractor.test.mjs`
- Test: `extension/tests/api.test.mjs`
- Test: `extension/tests/ui.test.mjs`

**Web (Next.js)**
- Modify: `web/src/app/page.tsx`
- Create: `web/src/app/follows/page.tsx`
- Create: `web/src/app/audits/page.tsx`
- Modify: `web/src/lib/dashboard.js`
- Create: `web/src/lib/follows.js`
- Create: `web/src/lib/audits.js`
- Create: `web/src/lib/jobActions.js`
- Test: `web/tests/dashboard.test.mjs`
- Test: `web/tests/follows.test.mjs`
- Test: `web/tests/audits.test.mjs`
- Test: `web/tests/jobActions.test.mjs`

---

## Chunk 1: Server Rule Engine + Data Model + APIs

### Task 1: Add Rule Parsing + Risk/Automation Engine

**Files:**
- Create: `server/src/main/java/com/jobagent/server/service/SalaryParser.java`
- Create: `server/src/main/java/com/jobagent/server/service/ExperienceParser.java`
- Create: `server/src/main/java/com/jobagent/server/service/RiskRuleSet.java`
- Create: `server/src/main/java/com/jobagent/server/service/AutomationPolicy.java`
- Create: `server/src/main/java/com/jobagent/server/service/RuleResult.java`
- Create: `server/src/main/java/com/jobagent/server/service/RuleConfigParser.java`
- Create: `server/src/main/java/com/jobagent/server/service/RuleEngineService.java`
- Test: `server/src/test/java/com/jobagent/server/service/RuleEngineServiceTest.java`

- [ ] **Step 1: Write failing tests**

```java
@Test
void parses_salary_and_experience_ranges() {
  RuleConfigParser parser = new RuleConfigParser();
  var config = parser.parse("20-30k", "3-5年", List.of("上海"), List.of(), List.of());
  assertEquals(20000, config.salaryMin());
  assertEquals(30000, config.salaryMax());
  assertEquals(3, config.expMin());
  assertEquals(5, config.expMax());
}

@Test
void risk_tags_union_worker_and_rules() {
  RiskRuleSet riskRules = new RiskRuleSet();
  RuleEngineService engine = new RuleEngineService(riskRules, new AutomationPolicy());
  RuleResult result = engine.evaluate(
      RuleEngineService.job("外包 大小周", "公司A", "20-30k", "3-5年", "上海"),
      RuleEngineService.taskConfig("上海", "20-30k", "3-5年", List.of(), List.of()),
      List.of("外包")
  );
  assertTrue(result.riskTags().contains("外包"));
  assertTrue(result.riskTags().contains("大小周"));
}
```

- [ ] **Step 2: Run tests (expect fail)**

Run:
```bash
cd server
mvn -Dmaven.repo.local=../.m2repo -Dtest=RuleEngineServiceTest test
```
Expected: FAIL (classes missing).

- [ ] **Step 3: Implement minimal rule engine**

- `SalaryParser` parses `10-20k`, `20k+`, `面议`.
- `ExperienceParser` parses `1-3年`, `3年以上`, `经验不限`.
- `RiskRuleSet` holds keyword rules.
- `AutomationPolicy` returns `AUTO/Semi/Conservative` + `auto_send_allowed` flag based on risk.
- `RuleEngineService` merges worker risk tags with rule tags and produces `RuleResult`.
- `RuleResult` includes: `hard_filter_pass`, `risk_tags`, `automation_action`, `parsed_range` (salary/exp range).

- [ ] **Step 4: Run tests (expect pass)**

```bash
cd server
mvn -Dmaven.repo.local=../.m2repo -Dtest=RuleEngineServiceTest test
```

- [ ] **Step 5: Commit**

```bash
git add server/src/main/java/com/jobagent/server/service/*.java server/src/test/java/com/jobagent/server/service/RuleEngineServiceTest.java
git commit -m "规则引擎基础能力"
```

---

### Task 2: Persist Rule Config & Rule Results

**Files:**
- Modify: `server/src/main/java/com/jobagent/server/store/TaskEntity.java`
- Modify: `server/src/main/java/com/jobagent/server/store/JobMatchEntity.java`
- Modify: `server/src/main/java/com/jobagent/server/store/TaskStore.java`
- Modify: `server/src/main/java/com/jobagent/server/service/JobPostService.java`
- Test: `server/src/test/java/com/jobagent/server/store/TaskStoreTest.java`

- [ ] **Step 1: Write failing tests**

```java
@Test
void task_create_saves_rule_config_json() {
  TaskCreateRequest req = new TaskCreateRequest("PM", "上海", "20-30k", "3-5年", List.of(), List.of(), "AUTO", "策略");
  TaskResponse resp = store.create(req, userId);
  TaskEntity entity = repository.findById(resp.id()).orElseThrow();
  assertNotNull(entity.getRuleConfigJson());
}
```

- [ ] **Step 2: Run tests (expect fail)**

```bash
cd server
mvn -Dmaven.repo.local=../.m2repo -Dtest=TaskStoreTest test
```

- [ ] **Step 3: Implement minimal persistence**

- Add `ruleConfigJson` to `TaskEntity`.
- In `TaskStore.create/update`, build rule config via `RuleConfigParser` and persist.
- Add `ruleJson` to `JobMatchEntity`.
- In `JobPostService.handlePageReport`, after worker response, call `RuleEngineService` and store `rule_json` into `JobMatchEntity`.
- `rule_json` must包含：`hard_filter_pass`、`risk_tags`、`automation_action`、`parsed_range`。
- 若 worker 未返回 `parsed_job`，服务端使用本地解析兜底并写入 `parsed_range`。
- Update `analysis.risks` to use final risk tags.

- [ ] **Step 4: Run tests (expect pass)**

```bash
cd server
mvn -Dmaven.repo.local=../.m2repo -Dtest=TaskStoreTest test
```

- [ ] **Step 5: Commit**

```bash
git add server/src/main/java/com/jobagent/server/store/TaskEntity.java \
  server/src/main/java/com/jobagent/server/store/JobMatchEntity.java \
  server/src/main/java/com/jobagent/server/store/TaskStore.java \
  server/src/main/java/com/jobagent/server/service/JobPostService.java \
  server/src/test/java/com/jobagent/server/store/TaskStoreTest.java
git commit -m "持久化规则配置与规则结果"
```

---

### Task 2b: Task Status Update (PAUSED/COMPLETED/FAILED)

**Files:**
- Modify: `server/src/main/java/com/jobagent/server/dto/TaskUpdateRequest.java`
- Modify: `server/src/main/java/com/jobagent/server/store/TaskStore.java`
- Modify: `server/src/test/java/com/jobagent/server/TaskControllerTest.java`

- [ ] **Step 1: Write failing tests**

```java
@Test
void task_update_allows_status_change() throws Exception {
  String token = loginAndGetToken();
  String taskId = createTask(token);
  mockMvc.perform(patch("/api/tasks/{id}", taskId)
      .header("Authorization", "Bearer " + token)
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"status\":\"PAUSED\"}"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.task.status").value("PAUSED"));
}

@Test
void task_update_rejects_invalid_status() throws Exception {
  String token = loginAndGetToken();
  String taskId = createTask(token);
  mockMvc.perform(patch("/api/tasks/{id}", taskId)
      .header("Authorization", "Bearer " + token)
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"status\":\"INVALID\"}"))
    .andExpect(status().isBadRequest());
}

@Test
void task_update_requires_auth() throws Exception {
  mockMvc.perform(patch("/api/tasks/{id}", "task-1")
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"status\":\"PAUSED\"}"))
    .andExpect(status().isUnauthorized());
}
```

- [ ] **Step 2: Run tests (expect fail)**

```bash
cd server
mvn -Dmaven.repo.local=../.m2repo -Dtest=TaskControllerTest test
```

- [ ] **Step 3: Implement minimal status update**

- Add `status` to `TaskUpdateRequest`.
- In `TaskStore.update`, allow updating status when provided.
- Keep validation minimal (accept only PAUSED/COMPLETED/FAILED).

- [ ] **Step 4: Run tests (expect pass)**

```bash
cd server
mvn -Dmaven.repo.local=../.m2repo -Dtest=TaskControllerTest test
```

- [ ] **Step 5: Commit**

```bash
git add server/src/main/java/com/jobagent/server/dto/TaskUpdateRequest.java   server/src/main/java/com/jobagent/server/store/TaskStore.java   server/src/test/java/com/jobagent/server/TaskControllerTest.java
git commit -m "任务状态更新支持"
```

---

### Task 3: Job Actions (Follow/Ignore/Blacklist) + Follow List API

**Files:**
- Create: `server/src/main/java/com/jobagent/server/store/UserCompanyBlacklistEntity.java`
- Create: `server/src/main/java/com/jobagent/server/store/UserJobActionEntity.java`
- Create: `server/src/main/java/com/jobagent/server/repository/UserCompanyBlacklistRepository.java`
- Create: `server/src/main/java/com/jobagent/server/repository/UserJobActionRepository.java`
- Create: `server/src/main/java/com/jobagent/server/controller/JobActionController.java`
- Create: `server/src/main/java/com/jobagent/server/dto/FollowItem.java`
- Create: `server/src/main/java/com/jobagent/server/dto/FollowListResponse.java`
- Create: `server/src/main/java/com/jobagent/server/dto/BlacklistCompanyRequest.java`
- Test: `server/src/test/java/com/jobagent/server/controller/JobActionControllerTest.java`

- [ ] **Step 1: Write failing tests**

```java
@Test
void follow_job_creates_action() throws Exception {
  String token = loginAndGetToken();
  mockMvc.perform(post("/api/jobs/{id}/follow", jobId)
      .header("Authorization", "Bearer " + token))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.status").value("ok"))
    .andExpect(jsonPath("$.follow_item.job_post_id").exists());
}

@Test
void follow_is_idempotent() throws Exception {
  String token = loginAndGetToken();
  mockMvc.perform(post("/api/jobs/{id}/follow", jobId)
      .header("Authorization", "Bearer " + token))
    .andExpect(status().isOk());
  mockMvc.perform(post("/api/jobs/{id}/follow", jobId)
      .header("Authorization", "Bearer " + token))
    .andExpect(status().isOk());
}

@Test
void follow_list_is_paginated() throws Exception {
  String token = loginAndGetToken();
  mockMvc.perform(get("/api/follows?page=0&size=10")
      .header("Authorization", "Bearer " + token))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.items").isArray())
    .andExpect(jsonPath("$.page").value(0))
    .andExpect(jsonPath("$.size").value(10))
    .andExpect(jsonPath("$.total").exists());
}

@Test
void follow_requires_auth() throws Exception {
  mockMvc.perform(post("/api/jobs/{id}/follow", jobId))
    .andExpect(status().isUnauthorized());
}

@Test
void follows_requires_auth() throws Exception {
  mockMvc.perform(get("/api/follows?page=0&size=10"))
    .andExpect(status().isUnauthorized());
}

@Test
void blacklist_requires_company_name() throws Exception {
  String token = loginAndGetToken();
  mockMvc.perform(post("/api/blacklist/company")
      .header("Authorization", "Bearer " + token)
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"source\":\"zhipin\"}"))
    .andExpect(status().isBadRequest());
}

@Test
void blacklist_requires_source() throws Exception {
  String token = loginAndGetToken();
  mockMvc.perform(post("/api/blacklist/company")
      .header("Authorization", "Bearer " + token)
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"company_name\":\"公司A\"}"))
    .andExpect(status().isBadRequest());
}

@Test
void ignore_is_idempotent() throws Exception {
  String token = loginAndGetToken();
  mockMvc.perform(post("/api/jobs/{id}/ignore", jobId)
      .header("Authorization", "Bearer " + token))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.status").value("ok"));
  mockMvc.perform(post("/api/jobs/{id}/ignore", jobId)
      .header("Authorization", "Bearer " + token))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.status").value("ok"));
}

@Test
void blacklist_is_idempotent() throws Exception {
  String token = loginAndGetToken();
  mockMvc.perform(post("/api/blacklist/company")
      .header("Authorization", "Bearer " + token)
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"company_name\":\"公司A\",\"source\":\"zhipin\"}"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.status").value("ok"));
  mockMvc.perform(post("/api/blacklist/company")
      .header("Authorization", "Bearer " + token)
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"company_name\":\"公司A\",\"source\":\"zhipin\"}"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.status").value("ok"));
}

@Test
void ignore_requires_auth() throws Exception {
  mockMvc.perform(post("/api/jobs/{id}/ignore", jobId))
    .andExpect(status().isUnauthorized());
}

@Test
void blacklist_requires_auth() throws Exception {
  mockMvc.perform(post("/api/blacklist/company")
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"company_name\":\"公司A\",\"source\":\"zhipin\"}"))
    .andExpect(status().isUnauthorized());
}
```

- [ ] **Step 2: Run tests (expect fail)**

```bash
cd server
mvn -Dmaven.repo.local=../.m2repo -Dtest=JobActionControllerTest test
```

- [ ] **Step 3: Implement minimal APIs**

- `POST /api/jobs/{job_post_id}/follow`: upsert into `UserJobAction(FOLLOW)` (idempotent).
- `POST /api/jobs/{job_post_id}/ignore`: upsert into `UserJobAction(IGNORE)` and mark JobPost status ARCHIVED（用于实现“忽略即归档”语义）。
- `POST /api/blacklist/company`: upsert `UserCompanyBlacklist` (idempotent).
- `GET /api/follows`: return paginated follows from JobPost data, ordered by created_at desc.

- [ ] **Step 4: Run tests (expect pass)**

```bash
cd server
mvn -Dmaven.repo.local=../.m2repo -Dtest=JobActionControllerTest test
```

- [ ] **Step 5: Commit**

```bash
git add server/src/main/java/com/jobagent/server/controller/JobActionController.java \
  server/src/main/java/com/jobagent/server/store/UserCompanyBlacklistEntity.java \
  server/src/main/java/com/jobagent/server/store/UserJobActionEntity.java \
  server/src/main/java/com/jobagent/server/repository/UserCompanyBlacklistRepository.java \
  server/src/main/java/com/jobagent/server/repository/UserJobActionRepository.java \
  server/src/main/java/com/jobagent/server/dto/FollowItem.java \
  server/src/main/java/com/jobagent/server/dto/FollowListResponse.java \
  server/src/main/java/com/jobagent/server/dto/BlacklistCompanyRequest.java \
  server/src/test/java/com/jobagent/server/controller/JobActionControllerTest.java
git commit -m "关注/忽略/黑名单接口"
```

---

### Task 3c: Enrich Dashboard Items with Job Context

**Files:**
- Modify: `server/src/main/java/com/jobagent/server/dto/DraftItem.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/ReplyItem.java`
- Modify: `server/src/main/java/com/jobagent/server/store/DashboardStore.java`
- Modify: `server/src/test/java/com/jobagent/server/DashboardControllerTest.java`

- [ ] **Step 1: Write failing tests**

```java
@Test
void dashboard_drafts_include_job_post_id_and_company() throws Exception {
  String token = loginAndGetToken();
  mockMvc.perform(get("/api/dashboard")
      .header("Authorization", "Bearer " + token))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.drafts[0].job_post_id").exists())
    .andExpect(jsonPath("$.drafts[0].company").exists());
}
```

- [ ] **Step 2: Run tests (expect fail)**

```bash
cd server
mvn -Dmaven.repo.local=../.m2repo -Dtest=DashboardControllerTest test
```

- [ ] **Step 3: Implement minimal enrichment**

- Add `job_post_id` and `company` to `DraftItem` and `ReplyItem`.
- Update `DashboardStore` to resolve conversation -> job_post -> company for drafts/replies.

- [ ] **Step 4: Run tests (expect pass)**

```bash
cd server
mvn -Dmaven.repo.local=../.m2repo -Dtest=DashboardControllerTest test
```

- [ ] **Step 5: Commit**

```bash
git add server/src/main/java/com/jobagent/server/dto/DraftItem.java \
  server/src/main/java/com/jobagent/server/dto/ReplyItem.java \
  server/src/main/java/com/jobagent/server/store/DashboardStore.java \
  server/src/test/java/com/jobagent/server/DashboardControllerTest.java
git commit -m "工作台条目补齐岗位信息"
```

---

### Task 4: Audit List API

**Files:**
- Create: `server/src/main/java/com/jobagent/server/controller/AuditController.java`
- Create: `server/src/main/java/com/jobagent/server/dto/AuditItem.java`
- Create: `server/src/main/java/com/jobagent/server/dto/AuditListResponse.java`
- Test: `server/src/test/java/com/jobagent/server/controller/AuditControllerTest.java`

- [ ] **Step 1: Write failing tests**

```java
@Test
void list_audits_returns_paginated_items() throws Exception {
  String token = loginAndGetToken();
  mockMvc.perform(get("/api/audits?page=0&size=10")
      .header("Authorization", "Bearer " + token))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.items").isArray())
    .andExpect(jsonPath("$.page").value(0));
}

@Test
void audits_requires_auth() throws Exception {
  mockMvc.perform(get("/api/audits?page=0&size=10"))
    .andExpect(status().isUnauthorized());
}
```

- [ ] **Step 2: Run tests (expect fail)**

```bash
cd server
mvn -Dmaven.repo.local=../.m2repo -Dtest=AuditControllerTest test
```

- [ ] **Step 3: Implement minimal API**

- Query `AuditLogRepository` with paging and order by createdAt desc.
- Return envelope `{items, page, size, total}`.

- [ ] **Step 4: Run tests (expect pass)**

```bash
cd server
mvn -Dmaven.repo.local=../.m2repo -Dtest=AuditControllerTest test
```

- [ ] **Step 5: Commit**

```bash
git add server/src/main/java/com/jobagent/server/controller/AuditController.java \
  server/src/main/java/com/jobagent/server/dto/AuditItem.java \
  server/src/main/java/com/jobagent/server/dto/AuditListResponse.java \
  server/src/test/java/com/jobagent/server/controller/AuditControllerTest.java
git commit -m "审计日志查询接口"
```

---

### Task 5: Retention Job (90 days)

**Files:**
- Create: `server/src/main/java/com/jobagent/server/service/RetentionService.java`
- Modify: `server/src/main/java/com/jobagent/server/JobAgentServerApplication.java`
- Test: `server/src/test/java/com/jobagent/server/service/RetentionServiceTest.java`

- [ ] **Step 1: Write failing tests**

```java
@Test
void retention_deletes_records_older_than_90_days() {
  retentionService.purgeOldData();
  assertTrue(jobPostRepository.findAll().isEmpty());
  assertTrue(jobMatchRepository.findAll().isEmpty());
  assertTrue(messageRepository.findAll().isEmpty());
  assertTrue(messageDraftRepository.findAll().isEmpty());
  assertTrue(auditLogRepository.findAll().isEmpty());
}

@Test
void retention_keeps_user_preferences() {
  retentionService.purgeOldData();
  assertFalse(userJobActionRepository.findAll().isEmpty());
  assertFalse(userCompanyBlacklistRepository.findAll().isEmpty());
}
```

- [ ] **Step 2: Run tests (expect fail)**

```bash
cd server
mvn -Dmaven.repo.local=../.m2repo -Dtest=RetentionServiceTest test
```

- [ ] **Step 3: Implement minimal retention**

- Add `@EnableScheduling` to app.
- Add `@Scheduled(cron = "0 0 3 * * *")` to purge.
- Add repository delete methods by createdAt for:
  - `JobPost`, `JobMatch`, `Message`, `MessageDraft`, `AuditLog`
- Do not delete:
  - `UserJobAction`, `UserCompanyBlacklist`

- [ ] **Step 4: Run tests (expect pass)**

```bash
cd server
mvn -Dmaven.repo.local=../.m2repo -Dtest=RetentionServiceTest test
```

- [ ] **Step 5: Commit**

```bash
git add server/src/main/java/com/jobagent/server/service/RetentionService.java \
  server/src/main/java/com/jobagent/server/JobAgentServerApplication.java \
  server/src/test/java/com/jobagent/server/service/RetentionServiceTest.java
git commit -m "数据保留定时清理"
```

---

## Chunk 2: Worker Contract Enhancements

### Task 6: Extend Worker Responses (parsed_job, risk_tags, interview_draft)

**Files:**
- Modify: `worker/src/job_agent_worker/models.py`
- Modify: `worker/src/job_agent_worker/app.py`
- Modify: `worker/tests/test_worker_api.py`

- [ ] **Step 1: Write failing tests**

```python
def test_job_match_returns_parsed_job_and_risk_tags(self):
    response = self.client.post("/worker/job-match", headers=self.headers, json={...})
    payload = response.json()
    self.assertIn("parsed_job", payload)
    self.assertEqual(set(payload["parsed_job"].keys()),
                     {"salary_min", "salary_max", "exp_min", "exp_max"})
    self.assertIn("risk_tags", payload)

def test_draft_returns_interview_draft_when_intent_interview(self):
    response = self.client.post(
        "/worker/draft",
        headers=self.headers,
        json={
            "task_id":"t1",
            "stage":"DRAFT",
            "conversation":{"id":"c1","intent":"INTERVIEW"},
            "job_post":{"company":"公司A","title":"产品经理"},
            "resume":{"content":"经验"},
            "idempotency_key":"k-interview"
        },
    )
    payload = response.json()
    self.assertIn("interview_draft", payload)

def test_worker_requires_token_for_job_match(self):
    response = self.client.post("/worker/job-match", json={...})
    self.assertEqual(response.status_code, 401)

def test_worker_requires_token_for_draft(self):
    response = self.client.post("/worker/draft", json={...})
    self.assertEqual(response.status_code, 401)
```

- [ ] **Step 2: Run tests (expect fail)**

```bash
PYTHONPATH=worker/src python3 -m unittest worker/tests/test_worker_api.py
```

- [ ] **Step 3: Implement minimal changes**

- Add parsed_job in job-match response with `salary_min/salary_max/exp_min/exp_max`.
- Add risk_tags list (derived from jd_raw keywords).
- When conversation intent is INTERVIEW, `/worker/draft` returns `interview_draft` (面试邀约草稿).

- [ ] **Step 4: Run tests (expect pass)**

```bash
PYTHONPATH=worker/src python3 -m unittest worker/tests/test_worker_api.py
```

- [ ] **Step 5: Commit**

```bash
git add worker/src/job_agent_worker/models.py worker/src/job_agent_worker/app.py worker/tests/test_worker_api.py
git commit -m "Worker 扩展解析与风险输出"
```

---

## Chunk 3: Extension (Extraction + Auto Send + Actions)

### Task 7: List/Detail Extraction Completeness

**Files:**
- Modify: `extension/src/extractor.js`
- Test: `extension/tests/extractor.test.mjs`

- [ ] **Step 1: Write failing tests**

```javascript
test("extract list cards with salary/exp/city", () => {
  const payload = extractJobPayload();
  expect(payload.cards?.length).toBeGreaterThan(0);
});

test("extract chat messages with role", () => {
  const messages = extractChatMessages();
  const roles = messages.map((m) => m.role);
  expect(roles).toContain("hr");
  expect(roles).toContain("user");
});
```

- [ ] **Step 2: Run tests (expect fail)**

```bash
cd extension
node --test tests/extractor.test.mjs
```

- [ ] **Step 3: Implement minimal extraction**

- Add list page card extraction with title/company/salary/experience/city/external_id/url.
- Detail page adds jd_raw/salary/experience/city.
- Chat page messages include role (hr/user) based on DOM markers (class/data attrs).

- [ ] **Step 4: Run tests (expect pass)**

```bash
cd extension
node --test tests/extractor.test.mjs
```

- [ ] **Step 5: Commit**

```bash
git add extension/src/extractor.js extension/tests/extractor.test.mjs
git commit -m "插件补齐列表与详情抽取"
```

---

### Task 8: Auto Send + Action/Heartbeat Reporting

**Files:**
- Modify: `extension/src/content.js`
- Modify: `extension/src/background.js`
- Modify: `extension/src/api.js`
- Modify: `extension/src/ui.js`
- Modify: `extension/tests/ui.test.mjs`

- [ ] **Step 1: Write failing tests**

```javascript
test("auto send triggers send action report", () => {
  // simulate chat page with draft + auto_send
  expect(mockPostActionReport).toHaveBeenCalled();
});

test("heartbeat payload includes required fields", () => {
  const payload = buildHeartbeatPayload();
  expect(payload.user_id).toBeDefined();
  expect(payload.task_id).toBeDefined();
  expect(payload.tab_id).toBeDefined();
  expect(payload.status).toBeDefined();
  expect(payload.ts).toBeDefined();
});

test("auto send reports SEND and DELIVERED", () => {
  // simulate success path
  expect(mockPostActionReport).toHaveBeenCalledWith(expect.objectContaining({ action_type: "SEND" }));
  expect(mockPostActionReport).toHaveBeenCalledWith(expect.objectContaining({ action_type: "DELIVERED" }));
});

test("auto send reports FAILED when cannot send", () => {
  // simulate missing input/button
  expect(mockPostActionReport).toHaveBeenCalledWith(expect.objectContaining({ action_type: "FAILED" }));
});
```

- [ ] **Step 2: Run tests (expect fail)**

```bash
cd extension
node --test tests/ui.test.mjs
```

- [ ] **Step 3: Implement minimal logic**

- Auto send when `auto_send=true`, plugin online, and chat input/button exists.
- On send, call `/plugin/action/report` with `SEND` then `DELIVERED` (delayed).
- Heartbeat includes `user_id/task_id/tab_id/status/ts`.
- If auto send fails (no input/button or send error): do not retry, report `FAILED` action and show user hint.
- Add pause/disable toggle in popup; content/background respect it.

- [ ] **Step 4: Run tests (expect pass)**

```bash
cd extension
node --test tests/ui.test.mjs
```

- [ ] **Step 5: Commit**

```bash
git add extension/src/content.js extension/src/background.js extension/src/api.js extension/src/ui.js extension/tests/ui.test.mjs
git commit -m "插件自动发送与动作上报"
```

---

## Chunk 4: Web Workbench (Follow/Ignore/Audit)

### Task 9: Follow List Page

**Files:**
- Create: `web/src/app/follows/page.tsx`
- Create: `web/src/lib/follows.js`
- Test: `web/tests/follows.test.mjs`

- [ ] **Step 1: Write failing tests**

```javascript
import { fetchFollows } from "../src/lib/follows.js";

test("fetchFollows returns items and sends auth header", async () => {
  // mock fetch to return {items:[...], page:0, size:10, total:1}
  // assert Authorization header present
});

test("fetchFollows returns fallback on 401", async () => {
  // mock fetch ok=false
});
```

- [ ] **Step 2: Run tests (expect fail)**

```bash
cd web
node --test tests/follows.test.mjs
```

- [ ] **Step 3: Implement minimal page + lib**

- Fetch follows, render list with action buttons.

- [ ] **Step 4: Run tests (expect pass)**

```bash
cd web
node --test tests/follows.test.mjs
```

- [ ] **Step 5: Commit**

```bash
git add web/src/app/follows/page.tsx web/src/lib/follows.js web/tests/follows.test.mjs
git commit -m "工作台关注列表"
```

---

### Task 9b: Dashboard Actions (Follow/Ignore/Blacklist)

**Files:**
- Modify: `web/src/app/page.tsx`
- Create: `web/src/lib/jobActions.js`
- Test: `web/tests/jobActions.test.mjs`

- [ ] **Step 1: Write failing tests**

```javascript
import { followJob, ignoreJob, blacklistCompany } from "../src/lib/jobActions.js";

test("followJob posts to follow endpoint with auth", async () => {
  // mock fetch, assert Authorization header and path
});

test("ignoreJob posts to ignore endpoint with auth", async () => {
  // mock fetch, assert Authorization header and path
});

test("blacklistCompany posts to blacklist endpoint with auth", async () => {
  // mock fetch, assert Authorization header and path
});
```

- [ ] **Step 2: Run tests (expect fail)**

```bash
cd web
node --test tests/jobActions.test.mjs
```

- [ ] **Step 3: Implement minimal actions**

- Add follow/ignore/blacklist buttons to recommendation/draft/reply lists in `page.tsx`.
- Use `job_post_id` and `company` from dashboard items (via Task 3c enrichment) to call APIs.
- If required fields are missing, disable the action button and show a hint.

- [ ] **Step 4: Run tests (expect pass)**

```bash
cd web
node --test tests/jobActions.test.mjs
```

- [ ] **Step 5: Commit**

```bash
git add web/src/app/page.tsx web/src/lib/jobActions.js web/tests/jobActions.test.mjs
git commit -m "工作台关注/忽略/黑名单操作"
```

---

### Task 10: Audit History Page

**Files:**
- Create: `web/src/app/audits/page.tsx`
- Create: `web/src/lib/audits.js`
- Test: `web/tests/audits.test.mjs`

- [ ] **Step 1: Write failing tests**

```javascript
import { fetchAudits } from "../src/lib/audits.js";

test("fetchAudits returns paginated data and sends auth header", async () => {
  // mock fetch {items:[], page:0, size:10, total:0} and assert page/size/total
});

test("fetchAudits returns fallback on 401", async () => {
  // mock fetch ok=false
});
```

- [ ] **Step 2: Run tests (expect fail)**

```bash
cd web
node --test tests/audits.test.mjs
```

- [ ] **Step 3: Implement minimal page + lib**

- Render table with action_type/created_at/result/payload/model_output/risk_tags.

- [ ] **Step 4: Run tests (expect pass)**

```bash
cd web
node --test tests/audits.test.mjs
```

- [ ] **Step 5: Commit**

```bash
git add web/src/app/audits/page.tsx web/src/lib/audits.js web/tests/audits.test.mjs
git commit -m "工作台历史审计"
```

---

## Chunk 5: Integration Updates + Final Tests

### Task 11: Wire API fields for auto-send and rule outputs

**Files:**
- Modify: `server/src/main/java/com/jobagent/server/dto/ChatReportResponse.java`
- Modify: `server/src/main/java/com/jobagent/server/service/ConversationService.java`
- Modify: `extension/src/content.js`
- Modify: `extension/src/api.js`
- Modify: `extension/src/ui.js`
- Test: `server/src/test/java/com/jobagent/server/PluginGatewayControllerTest.java`

- [ ] **Step 1: Write failing tests**

```java
@Test
void chat_report_returns_auto_send_hint_when_allowed() throws Exception {
  // build chat report
  // expect jsonPath("$.auto_send").value(true)
  // expect jsonPath("$.draft").exists()
}

@Test
void chat_report_auto_send_false_for_high_risk_or_non_auto() throws Exception {
  // build chat report with risk tags or automation_level != AUTO
  // expect jsonPath("$.auto_send").value(false)
}
```

- [ ] **Step 2: Run tests (expect fail)**

```bash
cd server
mvn -Dmaven.repo.local=../.m2repo -Dtest=PluginGatewayControllerTest test
```

- [ ] **Step 3: Implement minimal glue**

- Add `auto_send` + `draft` or `action_hint` to ChatReportResponse.
- ConversationService uses rule engine outputs to set auto_send when AUTO + low risk.
- Plugin reads auto_send and triggers send.

- [ ] **Step 4: Run tests (expect pass)**

```bash
cd server
mvn -Dmaven.repo.local=../.m2repo -Dtest=PluginGatewayControllerTest test
```

- [ ] **Step 5: Commit**

```bash
git add server/src/main/java/com/jobagent/server/dto/ChatReportResponse.java \
  server/src/main/java/com/jobagent/server/service/ConversationService.java \
  extension/src/content.js extension/src/api.js extension/src/ui.js \
  server/src/test/java/com/jobagent/server/PluginGatewayControllerTest.java
git commit -m "自动发送联动"
```

---

### Task 12: Full Test Sweep

- [ ] **Step 1: Run server tests**

```bash
cd server
mvn -Dmaven.repo.local=../.m2repo test
```

- [ ] **Step 2: Run worker tests**

```bash
PYTHONPATH=worker/src python3 -m unittest worker/tests/test_worker_api.py
```

- [ ] **Step 3: Run web tests**

```bash
cd web
node --test tests/dashboard.test.mjs tests/auth.test.mjs tests/tasks.test.mjs tests/follows.test.mjs tests/audits.test.mjs tests/jobActions.test.mjs
```

- [ ] **Step 4: Run extension tests**

```bash
cd extension
node --test tests/extractor.test.mjs tests/api.test.mjs tests/ui.test.mjs
```

- [ ] **Step 5: Commit (if needed for test-only updates)**

```bash
git status -sb
```

---

## Plan Review Loop
- After writing each chunk, dispatch plan-document-reviewer.
- Fix issues before proceeding to next chunk.

---

Plan complete and saved to `docs/superpowers/plans/2026-03-15-gap-fill-plan.md`. Ready to execute?
