# Agent Full MVP Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the full MVP job-agent flow (plugin + server + worker + workbench) per the approved spec.

**Architecture:** Spring Boot provides API + persistence + orchestration, a Python worker provides the scoring/draft/classify endpoints, and the Chrome MV3 extension performs page extraction + UI injection with the web workbench consuming server APIs for visibility and actions.

**Tech Stack:** Spring Boot (JPA/H2), Python (FastAPI), Next.js (app router), Chrome Extension MV3, Node test runner.

---

## File Map (Planned Changes)

**Server (Spring Boot)**
- Create: `server/src/main/java/com/jobagent/server/controller/AuthController.java`
- Create: `server/src/main/java/com/jobagent/server/controller/ConversationController.java`
- Create: `server/src/main/java/com/jobagent/server/controller/DraftController.java`
- Create: `server/src/main/java/com/jobagent/server/dto/Auth*.java`
- Create: `server/src/main/java/com/jobagent/server/dto/Conversation*.java`
- Create: `server/src/main/java/com/jobagent/server/dto/Draft*.java`
- Create: `server/src/main/java/com/jobagent/server/dto/Worker*.java`
- Create: `server/src/main/java/com/jobagent/server/repository/*.java` (new domain repos)
- Create: `server/src/main/java/com/jobagent/server/service/AuthService.java`
- Create: `server/src/main/java/com/jobagent/server/service/ModelOutputValidator.java`
- Create: `server/src/main/java/com/jobagent/server/service/WorkerClient.java`
- Create: `server/src/main/java/com/jobagent/server/service/JobPostService.java`
- Create: `server/src/main/java/com/jobagent/server/service/ConversationService.java`
- Create: `server/src/main/java/com/jobagent/server/service/AuditService.java`
- Create: `server/src/main/java/com/jobagent/server/service/IdempotencyKeys.java`
- Create: `server/src/main/java/com/jobagent/server/service/DuplicatePayloadBuilder.java`
- Create: `server/src/main/java/com/jobagent/server/service/DuplicateResponseException.java`
- Create: `server/src/main/java/com/jobagent/server/controller/PluginErrorHandler.java`
- Create: `server/src/main/java/com/jobagent/server/controller/ApiErrorHandler.java`
- Modify: `server/src/main/java/com/jobagent/server/controller/PluginGatewayController.java`
- Modify: `server/src/main/java/com/jobagent/server/controller/TaskController.java`
- Modify: `server/src/main/java/com/jobagent/server/controller/ResumeController.java`
- Modify: `server/src/main/java/com/jobagent/server/store/TaskEntity.java`
- Modify: `server/src/main/java/com/jobagent/server/store/TaskStore.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/TaskCreateRequest.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/TaskResponse.java`
- Create: `server/src/main/java/com/jobagent/server/dto/TaskCreateResponse.java`
- Create: `server/src/main/java/com/jobagent/server/dto/TaskListResponse.java`
- Create: `server/src/main/java/com/jobagent/server/dto/TaskUpdateRequest.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/PageReportRequest.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/PageReportResponse.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/ChatReportRequest.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/ChatReportResponse.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/DashboardResponse.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/DashboardMetrics.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/RecommendationItem.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/DraftItem.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/ReplyItem.java`
- Create: `server/src/main/java/com/jobagent/server/dto/InterviewItem.java`
- Modify: `server/src/main/java/com/jobagent/server/service/PluginAnalysisService.java` (remove stub logic)
- Modify: `server/src/test/java/com/jobagent/server/*` (add new tests and update existing)
- Modify: `server/src/main/resources/application.yml`
- Modify: `server/src/main/java/com/jobagent/server/store/DashboardRecommendationEntity.java`
- Modify: `server/src/main/java/com/jobagent/server/store/DashboardDraftEntity.java`
- Modify: `server/src/main/java/com/jobagent/server/store/DashboardReplyEntity.java`
- Modify: `server/src/main/java/com/jobagent/server/repository/DashboardRecommendationRepository.java`
- Modify: `server/src/main/java/com/jobagent/server/repository/DashboardDraftRepository.java`
- Modify: `server/src/main/java/com/jobagent/server/repository/DashboardReplyRepository.java`

**Worker (Python)**
- Create: `worker/src/job_agent_worker/app.py`
- Create: `worker/src/job_agent_worker/models.py`
- Modify: `worker/src/job_agent_worker/graphs.py`
- Create: `worker/tests/test_worker_api.py`

**Extension (MV3)**
- Create: `extension/src/api.js`
- Create: `extension/src/extractor.js`
- Create: `extension/src/ui.js`
- Modify: `extension/src/content.js`
- Modify: `extension/src/background.js`
- Modify: `extension/src/popup.html`
- Modify: `extension/src/popup.js`
- Modify: `extension/src/styles.css`
- Create: `extension/tests/extractor.test.mjs`

**Web (Next.js)**
- Create: `web/src/app/login/page.tsx`
- Create: `web/src/app/profile/page.tsx`
- Create: `web/src/app/resume/page.tsx`
- Create: `web/src/app/tasks/page.tsx`
- Modify: `web/src/app/page.tsx`
- Modify: `web/src/lib/dashboard.js`
- Modify: `web/src/lib/tasks.js`
- Create: `web/src/lib/auth.js`
- Create: `web/src/lib/resume.js`

---

## Chunk 1: Auth + Tasks + Resume

### Task 1: Auth Domain + Full Token Lifecycle

**Files:**
- Create: `server/src/main/java/com/jobagent/server/controller/AuthController.java`
- Create: `server/src/main/java/com/jobagent/server/service/AuthService.java`
- Create: `server/src/main/java/com/jobagent/server/service/TokenService.java`
- Create: `server/src/main/java/com/jobagent/server/dto/AuthLoginRequest.java`
- Create: `server/src/main/java/com/jobagent/server/dto/AuthLoginResponse.java`
- Create: `server/src/main/java/com/jobagent/server/dto/AuthRegisterRequest.java`
- Create: `server/src/main/java/com/jobagent/server/dto/AuthRegisterResponse.java`
- Create: `server/src/main/java/com/jobagent/server/dto/PluginTokenRequest.java`
- Create: `server/src/main/java/com/jobagent/server/dto/PluginTokenResponse.java`
- Create: `server/src/main/java/com/jobagent/server/dto/PluginTokenRefreshRequest.java`
- Create: `server/src/main/java/com/jobagent/server/dto/PluginTokenRevokeRequest.java`
- Create: `server/src/main/java/com/jobagent/server/repository/UserRepository.java`
- Create: `server/src/main/java/com/jobagent/server/repository/PluginTokenRepository.java`
- Create: `server/src/main/java/com/jobagent/server/store/UserEntity.java`
- Create: `server/src/main/java/com/jobagent/server/store/PluginTokenEntity.java`
- Test: `server/src/test/java/com/jobagent/server/AuthControllerTest.java`

- [ ] **Step 1: Write the failing test**

```java
@SpringBootTest(classes = JobAgentServerApplication.class)
@AutoConfigureMockMvc
class AuthControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper mapper;

  @Test
  void auth_flow_register_login_refresh_revoke() throws Exception {
    mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(Map.of(
            "account","alice","password","pwd123","email","a@x.com"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.user.id").exists());

    String loginResp = mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(Map.of("account","alice","password","pwd123"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.access_token").exists())
      .andExpect(jsonPath("$.refresh_token").exists())
      .andExpect(jsonPath("$.expires_in").exists())
      .andReturn().getResponse().getContentAsString();

    String accessToken = JsonPath.read(loginResp, "$.access_token");

    String pluginResp = mockMvc.perform(post("/api/auth/plugin/token")
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(Map.of("access_token", accessToken, "browser_id","chrome-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.plugin_token").exists())
      .andExpect(jsonPath("$.expires_in").value(86400))
      .andReturn().getResponse().getContentAsString();

    String pluginToken = JsonPath.read(pluginResp, "$.plugin_token");

    mockMvc.perform(post("/api/auth/plugin/refresh")
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(Map.of("plugin_token", pluginToken))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.plugin_token").exists())
      .andExpect(jsonPath("$.expires_in").value(86400));

    mockMvc.perform(post("/api/auth/plugin/revoke")
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(Map.of("plugin_token", pluginToken))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("ok"));
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `/Users/lushiwu/dev/apache-maven-3.9.4/bin/mvn -Dmaven.repo.local=/Users/lushiwu/Documents/job-agent-mvp/.m2repo test -Dtest=AuthControllerTest`

Expected: FAIL (auth endpoints missing)

- [ ] **Step 3: Write minimal implementation**

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService authService;

  @PostMapping("/register")
  public AuthRegisterResponse register(@RequestBody AuthRegisterRequest request) {
    return authService.register(request);
  }

  @PostMapping("/login")
  public AuthLoginResponse login(@RequestBody AuthLoginRequest request) {
    return authService.login(request);
  }

  @PostMapping("/plugin/token")
  public PluginTokenResponse issuePluginToken(@RequestBody PluginTokenRequest request) {
    return authService.issuePluginToken(request);
  }

  @PostMapping("/plugin/refresh")
  public PluginTokenResponse refresh(@RequestBody PluginTokenRefreshRequest request) {
    return authService.refreshPluginToken(request);
  }

@PostMapping("/plugin/revoke")
public StatusResponse revoke(@RequestBody PluginTokenRevokeRequest request) {
  authService.revokePluginToken(request);
  return new StatusResponse("ok");
}
}
```

```java
// TokenService: issue plugin token bound to browser_id with 24h TTL (expires_in=86400)
public PluginTokenResponse issuePluginToken(PluginTokenRequest request) {
  return tokenService.issuePluginToken(request.accessToken(), request.browserId());
}
```

```java
// Audit auth writes
auditService.record(userId, "AUTH_REGISTER", mapper.writeValueAsString(request));
auditService.record(userId, "AUTH_LOGIN", mapper.writeValueAsString(request));
auditService.record(userId, "PLUGIN_TOKEN_ISSUE", mapper.writeValueAsString(request));
auditService.record(userId, "PLUGIN_TOKEN_REFRESH", mapper.writeValueAsString(request));
auditService.record(userId, "PLUGIN_TOKEN_REVOKE", mapper.writeValueAsString(request));
```

```java
@Entity
@Table(name = "plugin_tokens")
public class PluginTokenEntity {
  @Id private String id;
  private String userId;
  private String browserId;
  private String token;
  private Instant expiresAt;
  private boolean revoked;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `/Users/lushiwu/dev/apache-maven-3.9.4/bin/mvn -Dmaven.repo.local=/Users/lushiwu/Documents/job-agent-mvp/.m2repo test -Dtest=AuthControllerTest`

Expected: PASS

- [ ] **Step 5: Checkpoint (git disabled)**

Confirm files saved and test output recorded.

### Task 2: Task APIs (Create/List/Patch) + Strategy JSON via GoalGraph

**Files:**
- Modify: `server/src/main/java/com/jobagent/server/store/TaskEntity.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/TaskCreateRequest.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/TaskResponse.java`
- Modify: `server/src/main/java/com/jobagent/server/store/TaskStore.java`
- Modify: `server/src/main/java/com/jobagent/server/controller/TaskController.java`
- Modify: `server/src/main/java/com/jobagent/server/repository/TaskRepository.java`
- Create: `server/src/main/java/com/jobagent/server/service/StrategyService.java`
- Create: `server/src/main/java/com/jobagent/server/dto/TaskUpdateRequest.java`
- Create: `server/src/main/java/com/jobagent/server/dto/TaskCreateResponse.java`
- Create: `server/src/main/java/com/jobagent/server/dto/TaskListResponse.java`
- Test: `server/src/test/java/com/jobagent/server/TaskControllerTest.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void create_and_patch_task_regenerates_strategy() throws Exception {
  String token = mockMvc.perform(post("/api/auth/register")
      .contentType(MediaType.APPLICATION_JSON)
      .content(mapper.writeValueAsString(Map.of("account","user","password","pwd"))))
    .andExpect(status().isOk())
    .andReturn().getResponse().getContentAsString();

  String accessToken = mockMvc.perform(post("/api/auth/login")
      .contentType(MediaType.APPLICATION_JSON)
      .content(mapper.writeValueAsString(Map.of("account","user","password","pwd"))))
    .andReturn().getResponse().getContentAsString();

  String bearer = "Bearer " + JsonPath.read(accessToken, "$.access_token");

  String createBody = mapper.writeValueAsString(Map.of(
      "title","产品经理","city","上海","salary","20k-30k",
      "experience","5年","exclude",List.of("外包"),
      "preferences",List.of("B端"),"automation_level","SEMI",
      "strategy_text","优先 B 端、排除外包"));

  String createResp = mockMvc.perform(post("/api/tasks")
      .header("Authorization", bearer)
      .contentType(MediaType.APPLICATION_JSON).content(createBody))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.task.strategy_json").exists())
    .andExpect(jsonPath("$.task.created_at").exists())
    .andReturn().getResponse().getContentAsString();

  String taskId = JsonPath.read(createResp, "$.task.task_id");

  mockMvc.perform(patch("/api/tasks/{id}", taskId)
      .header("Authorization", bearer)
      .contentType(MediaType.APPLICATION_JSON)
      .content(mapper.writeValueAsString(Map.of("strategy_text","只看 B 端"))))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.strategy_json").exists());

  mockMvc.perform(get("/api/tasks").header("Authorization", bearer))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.tasks[0].task_id").exists());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `/Users/lushiwu/dev/apache-maven-3.9.4/bin/mvn -Dmaven.repo.local=/Users/lushiwu/Documents/job-agent-mvp/.m2repo test -Dtest=TaskControllerTest`

Expected: FAIL (missing PATCH + strategy_json)

- [ ] **Step 3: Implement TaskEntity/Store/Controller + StrategyService**

```java
public class TaskEntity {
  @Id private String id;
  private String userId;
  private String title;
  private String city;
  private String salary;
  private String experience;
  private String automationLevel;
  private String status;
  @Lob private String strategyJson;
  @Lob private String excludeJson;
  @Lob private String preferencesJson;
  private Instant createdAt;
}
```

```java
public TaskResponse update(String id, String userId, TaskUpdateRequest request) {
  TaskEntity entity = repository.findByIdAndUserId(id, userId).orElseThrow();
  if (request.strategyText() != null) {
    entity.setStrategyJson(strategyService.parse(request.strategyText(), entity.getId()));
  }
  if (request.title() != null) entity.setTitle(request.title());
  if (request.city() != null) entity.setCity(request.city());
  if (request.salary() != null) entity.setSalary(request.salary());
  if (request.experience() != null) entity.setExperience(request.experience());
  if (request.exclude() != null) entity.setExcludeJson(toJson(request.exclude()));
  if (request.preferences() != null) entity.setPreferencesJson(toJson(request.preferences()));
  if (request.automationLevel() != null) entity.setAutomationLevel(request.automationLevel());
  TaskResponse response = toResponse(repository.save(entity));
  auditService.record(response.taskId(), "TASK_UPDATE", mapper.writeValueAsString(request));
  return response;
}
```

```java
private TaskResponse toResponse(TaskEntity entity) {
  return new TaskResponse(
      entity.getId(), entity.getStatus(), entity.getTitle(), entity.getCity(),
      entity.getSalary(), entity.getExperience(), entity.getAutomationLevel(),
      entity.getStrategyJson(), entity.getCreatedAt().toString());
}
```

```java
public interface TaskRepository extends JpaRepository<TaskEntity, String> {
  Optional<TaskEntity> findByIdAndUserId(String id, String userId);
  List<TaskEntity> findAllByUserId(String userId);
}
```

```java
public record TaskResponse(
  String taskId,
  String status,
  String title,
  String city,
  String salary,
  String experience,
  String automationLevel,
  String strategyJson,
  String createdAt
) {}
```

```java
public TaskResponse create(TaskCreateRequest request, String userId) {
  String generatedId = UUID.randomUUID().toString();
  String strategyJson = strategyService.parse(request.strategyText(), generatedId);
  TaskEntity entity = new TaskEntity(
      generatedId, userId, request.title(), request.city(), request.salary(),
      request.experience(), request.automationLevel(), "ACTIVE",
      strategyJson, toJson(request.exclude()), toJson(request.preferences()), Instant.now());
  entity.setStrategyJson(strategyJson);
  return toResponse(repository.save(entity));
}
```

```java
private String toJson(Object value) {
  return objectMapper.writeValueAsString(value);
}
```

```java
// StrategyService delegates to WorkerClient GoalGraph
public String parse(String strategyText, String taskId) {
  return workerClient.parseGoal(new GoalParseRequest(
      taskId, "GOAL_PARSE", strategyText,
      IdempotencyKeys.goalParse(taskId, strategyText)));
}
```

_Note: GoalGraph worker endpoint is implemented in Chunk 2 (Task 7)._
_Spec updated to include `GOAL_PARSE` stage and `/worker/goal-parse`._

```java
public final class IdempotencyKeys {
  public static String goalParse(String taskId, String strategyText) {
    String normalized = strategyText == null ? "" : strategyText.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    String hash = sha256(normalized);
    return taskId + ":GOAL_PARSE:" + hash;
  }

  private static String sha256(String value) {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(hashed);
  }
}
```

```java
@PostMapping
public TaskCreateResponse create(@RequestHeader("Authorization") String authHeader,
    @RequestBody TaskCreateRequest request) {
  String userId = authService.requireUserId(authHeader);
  TaskResponse response = store.create(request, userId); // store uses StrategyService to generate strategy_json
  auditService.record(response.taskId(), "TASK_CREATE", mapper.writeValueAsString(request));
  return new TaskCreateResponse(response);
}

@GetMapping
public TaskListResponse list(@RequestHeader("Authorization") String authHeader) {
  String userId = authService.requireUserId(authHeader);
  return new TaskListResponse(store.listForUser(userId));
}

@PatchMapping("/{id}")
public TaskResponse update(@RequestHeader("Authorization") String authHeader,
    @PathVariable String id, @RequestBody TaskUpdateRequest request) {
  return store.update(id, authService.requireUserId(authHeader), request);
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `/Users/lushiwu/dev/apache-maven-3.9.4/bin/mvn -Dmaven.repo.local=/Users/lushiwu/Documents/job-agent-mvp/.m2repo test -Dtest=TaskControllerTest`

Expected: PASS

- [ ] **Step 5: Checkpoint (git disabled)**

Confirm files saved and test output recorded.

### Task 2b: Resume Upload/Get API

**Files:**
- Modify: `server/src/main/java/com/jobagent/server/controller/ResumeController.java`
- Modify: `server/src/main/java/com/jobagent/server/store/ResumeStore.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/ResumeRequest.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/ResumeResponse.java`
- Test: `server/src/test/java/com/jobagent/server/ResumeControllerTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void upload_and_fetch_resume() throws Exception {
  mockMvc.perform(post("/api/auth/register")
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"account\":\"user\",\"password\":\"pwd\"}"))
    .andExpect(status().isOk());

  String bearer = "Bearer " + JsonPath.read(
      mockMvc.perform(post("/api/auth/login")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"account\":\"user\",\"password\":\"pwd\"}"))
        .andReturn().getResponse().getContentAsString(), "$.access_token");

  mockMvc.perform(post("/api/resume")
      .header("Authorization", bearer)
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"content\":\"简历内容\",\"format\":\"TEXT\"}"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.resume.id").exists());

  mockMvc.perform(get("/api/resume").header("Authorization", bearer))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.resume.parsed_json").exists());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `/Users/lushiwu/dev/apache-maven-3.9.4/bin/mvn -Dmaven.repo.local=/Users/lushiwu/Documents/job-agent-mvp/.m2repo test -Dtest=ResumeControllerTest`

Expected: FAIL (response shape mismatch)

- [ ] **Step 3: Implement response wrapper + parsing stub**

```java
public record ResumeResponse(ResumePayload resume) {}
public record ResumePayload(String id, String parsedJson, String createdAt) {}
```

```java
// Audit resume upload
auditService.record(userId, "RESUME_UPLOAD", mapper.writeValueAsString(request));
```

```java
// Controller guard
authService.verifyAccessToken(authHeader);
```

- [ ] **Step 4: Run test to verify it passes**

Run: `/Users/lushiwu/dev/apache-maven-3.9.4/bin/mvn -Dmaven.repo.local=/Users/lushiwu/Documents/job-agent-mvp/.m2repo test -Dtest=ResumeControllerTest`

Expected: PASS

- [ ] **Step 5: Checkpoint (git disabled)**

Confirm files saved and test output recorded.

## Chunk 2: Domain + Plugin + Dashboard + Workbench

### Task 3: Domain Entities + Dedupe Constraints

**Files:**
- Create: `server/src/main/java/com/jobagent/server/store/JobPostEntity.java`
- Create: `server/src/main/java/com/jobagent/server/store/JobMatchEntity.java`
- Create: `server/src/main/java/com/jobagent/server/store/ConversationEntity.java`
- Create: `server/src/main/java/com/jobagent/server/store/MessageEntity.java`
- Create: `server/src/main/java/com/jobagent/server/store/MessageDraftEntity.java`
- Create: `server/src/main/java/com/jobagent/server/store/AuditLogEntity.java`
- Create: `server/src/main/java/com/jobagent/server/repository/*.java`
- Test: `server/src/test/java/com/jobagent/server/repository/JobDomainRepositoryTest.java`

- [ ] **Step 1: Write failing repository tests**

```java
@DataJpaTest
class JobDomainRepositoryTest {
  @Autowired JobPostRepository jobPostRepository;
  @Autowired ConversationRepository conversationRepository;

  @Test
  void dedupeByExternalId() {
    JobPostEntity post = new JobPostEntity(
        "1","t","zhipin","ext-1","产品","智聘","上海","20k","5年","JD","{}", "DISCOVERED", Instant.now());
    jobPostRepository.save(post);
    assertThat(jobPostRepository.findBySourceAndExternalId("zhipin","ext-1")).isPresent();
  }

  @Test
  void conversationUniqueKey() {
    ConversationEntity conv = new ConversationEntity("c1","t","p1","conv-1","NEW", null, null, null, Instant.now());
    conversationRepository.save(conv);
    assertThat(conversationRepository.findByTaskIdAndExternalId("t","conv-1")).isPresent();
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `/Users/lushiwu/dev/apache-maven-3.9.4/bin/mvn -Dmaven.repo.local=/Users/lushiwu/Documents/job-agent-mvp/.m2repo test -Dtest=JobDomainRepositoryTest`

Expected: FAIL (missing entities/repos)

- [ ] **Step 3: Implement entities + repositories with unique indexes**

```java
@Entity
@Table(name = "job_posts", uniqueConstraints = @UniqueConstraint(columnNames = {"source","external_id"}))
public class JobPostEntity {
  @Id private String id;
  private String taskId;
  private String source;
  @Column(name = "external_id") private String externalId;
  private String title;
  private String company;
  private String city;
  private String salary;
  private String experience;
  @Lob private String jdRaw;
  @Lob private String parsedJson;
  private String status;
  private Instant createdAt;

  protected JobPostEntity() {}
  public JobPostEntity(String id, String taskId, String source, String externalId, String title,
      String company, String city, String salary, String experience, String jdRaw,
      String parsedJson, String status, Instant createdAt) {
    this.id = id;
    this.taskId = taskId;
    this.source = source;
    this.externalId = externalId;
    this.title = title;
    this.company = company;
    this.city = city;
    this.salary = salary;
    this.experience = experience;
    this.jdRaw = jdRaw;
    this.parsedJson = parsedJson;
    this.status = status;
    this.createdAt = createdAt;
  }
}
```

_Note: All JPA entities follow the same pattern: protected no-arg constructor + full-args constructor used by tests._

_Accessor strategy (explicit methods):_

```java
// JobPostEntity accessors
public String getId() { return id; }
public String getTaskId() { return taskId; }
public String getExternalId() { return externalId; }
public String getStatus() { return status; }
public void setStatus(String status) { this.status = status; }
public Instant getCreatedAt() { return createdAt; }

// ConversationEntity accessors
public String getId() { return id; }
public String getStatus() { return status; }
public void setStatus(String status) { this.status = status; }
public void setLastIntent(String lastIntent) { this.lastIntent = lastIntent; }
public void setLastSummary(String lastSummary) { this.lastSummary = lastSummary; }
public void setLastAction(String lastAction) { this.lastAction = lastAction; }
public String getLastIntent() { return lastIntent; }
public String getLastSummary() { return lastSummary; }
public String getLastAction() { return lastAction; }

// MessageDraftEntity accessors
public String getId() { return id; }
public boolean isApproved() { return approved; }
public void setApproved(boolean approved) { this.approved = approved; }
public String getConversationId() { return conversationId; }
public String getContent() { return content; }
```

```java
@Entity
@Table(name = "conversations", uniqueConstraints = @UniqueConstraint(columnNames = {"task_id","external_id"}))
public class ConversationEntity {
  @Id private String id;
  @Column(name = "task_id") private String taskId;
  private String jobPostId;
  @Column(name = "external_id") private String externalId;
  private String status;
  private String lastIntent;
  private String lastSummary;
  private String lastAction;
  private Instant createdAt;

  protected ConversationEntity() {}
  public ConversationEntity(String id, String taskId, String jobPostId, String externalId,
      String status, String lastIntent, String lastSummary, String lastAction, Instant createdAt) {
    this.id = id;
    this.taskId = taskId;
    this.jobPostId = jobPostId;
    this.externalId = externalId;
    this.status = status;
    this.lastIntent = lastIntent;
    this.lastSummary = lastSummary;
    this.lastAction = lastAction;
    this.createdAt = createdAt;
  }
}
```

```java
@Entity
@Table(name = "messages", uniqueConstraints = @UniqueConstraint(columnNames = {"conversation_id","external_id"}))
public class MessageEntity {
  @Id private String id;
  @Column(name = "conversation_id") private String conversationId;
  private String role;
  private String content;
  @Column(name = "external_id") private String externalId;
  private Instant createdAt;

  protected MessageEntity() {}
  public MessageEntity(String id, String conversationId, String role, String content,
      String externalId, Instant createdAt) {
    this.id = id;
    this.conversationId = conversationId;
    this.role = role;
    this.content = content;
    this.externalId = externalId;
    this.createdAt = createdAt;
  }
}
```

```java
@Entity
@Table(name = "message_drafts", uniqueConstraints = @UniqueConstraint(columnNames = {"conversation_id","source_type"}))
public class MessageDraftEntity {
  @Id private String id;
  @Column(name = "conversation_id") private String conversationId;
  @Lob private String content;
  @Column(name = "source_type") private String sourceType;
  private boolean approved;
  private Instant createdAt;

  protected MessageDraftEntity() {}
  public MessageDraftEntity(String id, String conversationId, String content,
      String sourceType, boolean approved, Instant createdAt) {
    this.id = id;
    this.conversationId = conversationId;
    this.content = content;
    this.sourceType = sourceType;
    this.approved = approved;
    this.createdAt = createdAt;
  }
}
```

```java
@Entity
@Table(name = "job_matches")
public class JobMatchEntity {
  @Id private String id;
  private String taskId;
  private String jobPostId;
  private int score;
  @Lob private String reasonJson;
  @Lob private String riskTagsJson;
  private Instant createdAt;

  protected JobMatchEntity() {}
  public JobMatchEntity(String id, String taskId, String jobPostId, int score,
      String reasonJson, String riskTagsJson, Instant createdAt) {
    this.id = id;
    this.taskId = taskId;
    this.jobPostId = jobPostId;
    this.score = score;
    this.reasonJson = reasonJson;
    this.riskTagsJson = riskTagsJson;
    this.createdAt = createdAt;
  }
}
```

```java
public interface JobPostRepository extends JpaRepository<JobPostEntity, String> {
  Optional<JobPostEntity> findBySourceAndExternalId(String source, String externalId);
}

public interface ConversationRepository extends JpaRepository<ConversationEntity, String> {
  Optional<ConversationEntity> findByTaskIdAndExternalId(String taskId, String externalId);
}
```

```java
public interface JobMatchRepository extends JpaRepository<JobMatchEntity, String> {
  Optional<JobMatchEntity> findByJobPostId(String jobPostId);
}

public interface MessageRepository extends JpaRepository<MessageEntity, String> {
  Optional<MessageEntity> findByConversationIdAndExternalId(String conversationId, String externalId);
}

public interface MessageDraftRepository extends JpaRepository<MessageDraftEntity, String> {
  Optional<MessageDraftEntity> findByConversationIdAndSourceType(String conversationId, String sourceType);
}
```

```java
@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {
  @Id private String id;
  private String userId;
  private String actionType;
  @Lob private String payload;
  private Instant createdAt;

  protected AuditLogEntity() {}
  public AuditLogEntity(String id, String userId, String actionType, String payload, Instant createdAt) {
    this.id = id;
    this.userId = userId;
    this.actionType = actionType;
    this.payload = payload;
    this.createdAt = createdAt;
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `/Users/lushiwu/dev/apache-maven-3.9.4/bin/mvn -Dmaven.repo.local=/Users/lushiwu/Documents/job-agent-mvp/.m2repo test -Dtest=JobDomainRepositoryTest`

Expected: PASS

- [ ] **Step 5: Checkpoint (git disabled)**

Confirm files saved and test output recorded.

### Task 4: Worker Client + Validation Rules + Idempotency

**Files:**
- Create: `server/src/main/java/com/jobagent/server/service/WorkerClient.java`
- Create: `server/src/main/java/com/jobagent/server/service/ModelOutputValidator.java`
- Create: `server/src/main/java/com/jobagent/server/dto/Worker*.java`
- Test: `server/src/test/java/com/jobagent/server/WorkerClientTest.java`
- Test: `server/src/test/java/com/jobagent/server/ModelOutputValidatorTest.java`
- Modify: `server/src/main/resources/application.yml`

- [ ] **Step 1: Write failing tests**

```java
@SpringBootTest
class ModelOutputValidatorTest {
  @Test
  void rejectsContactAndOverlength() {
    ModelOutputValidator validator = new ModelOutputValidator();
    assertThatThrownBy(() -> validator.validateDraft("加我微信 abc"))
      .isInstanceOf(ValidationException.class);
    assertThatThrownBy(() -> validator.validateSummary("x".repeat(300)))
      .isInstanceOf(ValidationException.class);
  }
}
```

```java
@RestClientTest(WorkerClient.class)
class WorkerClientTest {
  @Autowired private WorkerClient client;
  @Autowired private MockRestServiceServer server;

  @Test
  void sendsAuthHeaderAndIdempotencyKey() {
    server.expect(requestTo("http://worker/worker/job-match"))
      .andExpect(header("X-Worker-Token", "worker-secret"))
      .andExpect(content().string(containsString("\"idempotency_key\"")))
      .andRespond(withSuccess("{\"score\":80,\"reasons\":[],\"risks\":[]}", MediaType.APPLICATION_JSON));

    client.jobMatch(new WorkerJobMatchRequest(
        "t1","JOB_MATCH",
        Map.of("external_id","1","source","zhipin","jd_raw","产品"),
        Map.of("content","产品"),
        Map.of("keywords",List.of("产品")),
        "k1"));
  }

  @Test
  void parsesGoal() {
    server.expect(requestTo("http://worker/worker/goal-parse"))
      .andExpect(header("X-Worker-Token", "worker-secret"))
      .andRespond(withSuccess("{\"strategy_json\":\"{\\\"keywords\\\":[\\\"产品\\\"]}\"}", MediaType.APPLICATION_JSON));
    client.parseGoal(new GoalParseRequest("t1","GOAL_PARSE","策略","k2"));
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `/Users/lushiwu/dev/apache-maven-3.9.4/bin/mvn -Dmaven.repo.local=/Users/lushiwu/Documents/job-agent-mvp/.m2repo test -Dtest=ModelOutputValidatorTest,WorkerClientTest`

Expected: FAIL

- [ ] **Step 3: Implement validator + worker client**

```java
public void validateDraft(String content) {
  if (content.length() < 10 || content.length() > 500) throw new ValidationException();
  if (content.matches(".*(微信|QQ|@|\\d{11}|\\w+@\\w+).*")) throw new ValidationException();
  if (content.matches(".*(保证录用|100%录取).*")) throw new ValidationException();
  if (sensitiveWordList.stream().anyMatch(content::contains)) throw new ValidationException();
}
```

```java
public void validateSummary(String summary) {
  if (summary.length() > 200) throw new ValidationException();
  if (summary.matches(".*(微信|QQ|@|\\d{11}|\\w+@\\w+).*")) throw new ValidationException();
  if (sensitiveWordList.stream().anyMatch(summary::contains)) throw new ValidationException();
  if (summary.matches(".*(保证录用|100%录取).*")) throw new ValidationException();
}
```

```java
// WorkerClient: 10s timeout, 2 retries, add X-Worker-Token header
for (int attempt = 0; attempt < 3; attempt++) {
  try { return restTemplate.postForObject(url, requestWithHeaders, Response.class); }
  catch (RestClientException ex) { if (attempt == 2) throw ex; }
}
```

```java
public String parseGoal(GoalParseRequest request) {
  return post("/worker/goal-parse", request).strategyJson();
}
```

```java
// WorkerClient config (application.yml)
job-agent:
  worker:
    base-url: http://worker
    token: worker-secret
```

- [ ] **Step 4: Run test to verify it passes**

Run: `/Users/lushiwu/dev/apache-maven-3.9.4/bin/mvn -Dmaven.repo.local=/Users/lushiwu/Documents/job-agent-mvp/.m2repo test -Dtest=ModelOutputValidatorTest,WorkerClientTest`

Expected: PASS

- [ ] **Step 5: Checkpoint (git disabled)**

Confirm files saved and test output recorded.

### Task 5b: Dashboard Snapshot API

**Files:**
- Modify: `server/src/main/java/com/jobagent/server/controller/DashboardController.java`
- Modify: `server/src/main/java/com/jobagent/server/store/DashboardStore.java`
- Test: `server/src/test/java/com/jobagent/server/DashboardControllerTest.java`

- [ ] **Step 1: Write failing test**

```java
@Test
void dashboard_returns_metrics_and_lists() throws Exception {
  mockMvc.perform(post("/api/auth/register")
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"account\":\"user\",\"password\":\"pwd\"}"))
    .andExpect(status().isOk());

  String bearer = "Bearer " + JsonPath.read(
      mockMvc.perform(post("/api/auth/login")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"account\":\"user\",\"password\":\"pwd\"}"))
        .andReturn().getResponse().getContentAsString(), "$.access_token");

  mockMvc.perform(get("/api/dashboard").header("Authorization", bearer))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.metrics.recommendations").exists())
    .andExpect(jsonPath("$.recommendations").isArray())
    .andExpect(jsonPath("$.interviews").isArray())
    .andExpect(jsonPath("$.updated_at").exists());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `/Users/lushiwu/dev/apache-maven-3.9.4/bin/mvn -Dmaven.repo.local=/Users/lushiwu/Documents/job-agent-mvp/.m2repo test -Dtest=DashboardControllerTest`

Expected: FAIL (auth missing/shape mismatch)

- [ ] **Step 3: Implement aggregation**

```java
public DashboardResponse snapshot(String userId) {
  List<DashboardRecommendationEntity> recs = recommendationRepository.findTopByUserId(userId);
  return new DashboardResponse(metrics, recs, drafts, replies, interviews, Instant.now().toString());
}
```

```java
// Ensure response includes interviews and updated_at + required item fields
DashboardResponse response = new DashboardResponse(
  new DashboardMetrics(recommendations.size(), drafts.size(), replies.size(), interviews.size()),
  recommendations, drafts, replies, interviews, Instant.now().toString()
);
```

```java
public record DashboardResponse(
  DashboardMetrics metrics,
  List<RecommendationItem> recommendations,
  List<DraftItem> drafts,
  List<ReplyItem> replies,
  List<InterviewItem> interviews,
  String updatedAt
) {}

public record RecommendationItem(String jobPostId, String title, String company, int score, List<String> risks, String status) {}
public record DraftItem(String draftId, String conversationId, String content, String createdAt, boolean approved) {}
public record ReplyItem(String conversationId, String summary, String intent, String updatedAt) {}
public record InterviewItem(String conversationId, String company, String title, String scheduledAt) {}
```

_Note: JSON uses global SNAKE_CASE; `updatedAt` -> `updated_at`, etc._

```java
@GetMapping("/dashboard")
public DashboardResponse dashboard(@RequestHeader("Authorization") String authHeader) {
  String userId = authService.requireUserId(authHeader);
  return store.snapshot(userId);
}
```

```java
// Dashboard entities include userId for scoping
@Entity
public class DashboardRecommendationEntity {
  @Id private String id;
  private String userId;
  private String title;
  private String company;
  private int score;
  @Lob private String risksJson;
  private String status;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `/Users/lushiwu/dev/apache-maven-3.9.4/bin/mvn -Dmaven.repo.local=/Users/lushiwu/Documents/job-agent-mvp/.m2repo test -Dtest=DashboardControllerTest`

Expected: PASS

- [ ] **Step 5: Checkpoint (git disabled)**

Confirm files saved and test output recorded.

### Task 5: Plugin Gateway Endpoints + Error Handling + Audit Logs

**Files:**
- Modify: `server/src/main/java/com/jobagent/server/controller/PluginGatewayController.java`
- Modify: `server/src/main/java/com/jobagent/server/service/JobPostService.java`
- Modify: `server/src/main/java/com/jobagent/server/service/ConversationService.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/PageReportRequest.java`
- Modify: `server/src/main/java/com/jobagent/server/dto/ChatReportRequest.java`
- Create: `server/src/main/java/com/jobagent/server/service/AuditService.java`
- Create: `server/src/main/java/com/jobagent/server/service/DuplicatePayloadBuilder.java`
- Create: `server/src/main/java/com/jobagent/server/service/DuplicateResponseException.java`
- Create: `server/src/main/java/com/jobagent/server/controller/PluginErrorHandler.java`
- Create: `server/src/main/java/com/jobagent/server/controller/ApiErrorHandler.java`
- Test: `server/src/test/java/com/jobagent/server/PluginGatewayControllerTest.java`

- [ ] **Step 1: Write failing tests**

```java
@Test
void pluginEndpoints_requireToken_and_returnErrors() throws Exception {
  mockMvc.perform(post("/plugin/heartbeat")
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"status\":\"ok\"}"))
    .andExpect(status().isUnauthorized())
    .andExpect(jsonPath("$.code").value("PLUGIN_TOKEN_INVALID"));

  mockMvc.perform(post("/plugin/action/report")
      .header("X-Plugin-Token","token")
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"task_id\":\"t\",\"action_type\":\"SEND\",\"status\":\"ok\"}"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.status").value("ok"));

  mockMvc.perform(post("/plugin/chat/report")
      .header("X-Plugin-Token","token")
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"task_id\":\"t\",\"conversation_id\":\"c1\",\"messages\":[{\"id\":\"m1\",\"role\":\"hr\",\"text\":\"面试\"}],\"last_message_id\":\"m1\"}"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.reply.intent").exists());

  mockMvc.perform(post("/plugin/page/report")
      .header("X-Plugin-Token","token")
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"task_id\":\"t\",\"page_type\":\"detail\",\"raw_text\":\"x\",\"want_draft\":false,\"extracted_json\":{},\"source_url\":\"u\",\"dom_hash\":\"d\"}"))
    .andExpect(status().isOk());

  mockMvc.perform(post("/plugin/page/report")
      .header("X-Plugin-Token","token")
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"task_id\":\"t\",\"page_type\":\"detail\",\"raw_text\":\"x\",\"want_draft\":false,\"extracted_json\":{},\"source_url\":\"u\",\"dom_hash\":\"d\"}"))
    .andExpect(status().isConflict())
    .andExpect(jsonPath("$.code").value("DUPLICATE_IGNORED"))
    .andExpect(jsonPath("$.payload.analysis").exists());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `/Users/lushiwu/dev/apache-maven-3.9.4/bin/mvn -Dmaven.repo.local=/Users/lushiwu/Documents/job-agent-mvp/.m2repo test -Dtest=PluginGatewayControllerTest`

Expected: FAIL

- [ ] **Step 3: Implement endpoints + dedupe + state transitions**

```java
@PostMapping("/heartbeat")
public StatusResponse heartbeat(@RequestHeader("X-Plugin-Token") String token,
    @RequestBody HeartbeatRequest req) { authService.verifyPluginToken(token); return new StatusResponse("ok"); }
```

```java
// On duplicates return 409 with existing payload; on worker timeout return 504.
try {
  PageReportResponse response = jobPostService.handlePageReport(request);
  return response;
} catch (DuplicateResponseException ex) {
  throw new DuplicateResponseException(ex.payload());
}
```

```java
catch (ValidationException ex) {
  throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
}
```

```java
catch (ResourceAccessException ex) {
  throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "WORKER_TIMEOUT");
}
```

```java
@RestControllerAdvice
public class PluginErrorHandler {
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String, Object>> handle(ResponseStatusException ex) {
    String code = ex.getReason();
    if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED && code == null) {
      code = "PLUGIN_TOKEN_INVALID";
    }
    return ResponseEntity.status(ex.getStatusCode())
      .body(Map.of("code", code, "message", ex.getMessage()));
  }
}
```

```java
@ExceptionHandler(MissingRequestHeaderException.class)
public ResponseEntity<Map<String, Object>> handleMissingHeader() {
  return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
    .body(Map.of("code", "PLUGIN_TOKEN_INVALID", "message", "Missing plugin token"));
}
```

```java
@ExceptionHandler(DuplicateResponseException.class)
public ResponseEntity<Object> handleDuplicate(DuplicateResponseException ex) {
  return ResponseEntity.status(HttpStatus.CONFLICT)
    .body(Map.of("code", "DUPLICATE_IGNORED", "payload", ex.payload()));
}
```

```java
@RestControllerAdvice
@RequestMapping("/api")
public class ApiErrorHandler {
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String, Object>> handle(ResponseStatusException ex) {
    return ResponseEntity.status(ex.getStatusCode())
      .body(Map.of("error", Map.of("code", ex.getReason(), "message", ex.getMessage())));
  }
}
```

```java
@PostMapping("/action/report")
public StatusResponse actionReport(@RequestHeader("X-Plugin-Token") String token,
    @RequestBody ActionReportRequest req) {
  String userId = authService.requireUserIdFromPluginToken(token);
  auditService.record(userId, "PLUGIN_ACTION", mapper.writeValueAsString(req));
  return new StatusResponse("ok");
}
```

```java
// Audit log example
String userId = authService.requireUserIdFromPluginToken(token);
auditService.record(userId, "PLUGIN_PAGE_REPORT", mapper.writeValueAsString(request));
```

```java
auditService.record(userId, "PLUGIN_CHAT_REPORT", mapper.writeValueAsString(request));
```

```java
// State transitions
jobPost.setStatus("ANALYZED");
if (score >= 70) jobPost.setStatus("SHORTLISTED");
```

```java
// Chat-based transitions
if ("INTERVIEW".equals(reply.intent())) {
  conversation.setStatus("INTERVIEW");
  jobPost.setStatus("INTERVIEW");
} else if ("NEEDS_REPLY".equals(reply.intent())) {
  conversation.setStatus("NEEDS_REPLY");
  jobPost.setStatus("REPLIED");
} else if ("REJECTED".equals(reply.intent())) {
  conversation.setStatus("CLOSED");
  jobPost.setStatus("ARCHIVED");
}
conversation.setLastIntent(reply.intent());
conversation.setLastSummary(reply.summary());
conversation.setLastAction(reply.nextAction());
```

```java
// Draft generation transition
conversation.setStatus("WAITING_USER");
jobPost.setStatus("DRAFTED");

// Action report transitions
if ("SEND".equals(actionType)) {
  conversation.setStatus("SENT");
  jobPost.setStatus("SENT");
}
if ("DELIVERED".equals(actionType)) {
  conversation.setStatus("WAITING_HR");
}
```

```java
// Draft gating
DraftItem draft = null;
if ("detail".equals(request.pageType()) && Boolean.TRUE.equals(request.wantDraft())) {
  draft = workerClient.buildDraft(new WorkerDraftRequest(
      request.taskId(), "DRAFT", conversationPayload, jobPostPayload, resumePayload, draftKey));
}
```

```java
// Dedupe rules
Optional<JobPostEntity> existing = jobPostRepository.findBySourceAndExternalId(source, externalId);
if (existing.isPresent()) {
  JobMatchEntity match = jobMatchRepository.findByJobPostId(existing.get().getId()).orElse(null);
  PageReportResponse payload = duplicatePayloadBuilder.pageReport(existing.get(), match, null);
  throw new DuplicateResponseException(payload);
}
Optional<ConversationEntity> conv = conversationRepository.findByTaskIdAndExternalId(taskId, conversationId);
```

```java
// Message dedupe on chat report
for (MessagePayload message : request.messages()) {
  if (messageRepository.findByConversationIdAndExternalId(convId, message.id()).isPresent()) {
    continue;
  }
  messageRepository.save(new MessageEntity(UUID.randomUUID().toString(), convId,
      message.role(), message.text(), message.id(), Instant.now()));
}
```

```java
// Duplicate chat report: if last_message_id already stored, return existing reply payload
if (messageRepository.findByConversationIdAndExternalId(convId, request.lastMessageId()).isPresent()) {
  ChatReportResponse existing = duplicatePayloadBuilder.chatReport(convId);
  throw new DuplicateResponseException(existing);
}
```

```java
// duplicatePayloadBuilder.chatReport: builds from ConversationEntity.lastIntent/lastSummary/lastAction
```

```java
// Idempotency keys for worker calls
String matchKey = taskId + ":JOB_MATCH:" + externalId + ":" + source;
String draftKey = taskId + ":DRAFT:" + conversationId + ":" + externalId;
String replyKey = taskId + ":REPLY_CLASSIFY:" + conversationId + ":" + lastMessageId;
```

- [ ] **Step 4: Run test to verify it passes**

Run: `/Users/lushiwu/dev/apache-maven-3.9.4/bin/mvn -Dmaven.repo.local=/Users/lushiwu/Documents/job-agent-mvp/.m2repo test -Dtest=PluginGatewayControllerTest`

Expected: PASS

- [ ] **Step 5: Checkpoint (git disabled)**

Confirm files saved and test output recorded.

### Task 6: Workbench Actions + Conversation Detail

**Files:**
- Create: `server/src/main/java/com/jobagent/server/controller/ConversationController.java`
- Create: `server/src/main/java/com/jobagent/server/controller/DraftController.java`
- Test: `server/src/test/java/com/jobagent/server/ConversationControllerTest.java`

- [ ] **Step 1: Write failing tests**

```java
@Test
void approve_reject_close_regenerate() throws Exception {
  mockMvc.perform(post("/api/auth/register")
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"account\":\"user\",\"password\":\"pwd\"}"))
    .andExpect(status().isOk());

  String bearer = "Bearer " + JsonPath.read(
      mockMvc.perform(post("/api/auth/login")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"account\":\"user\",\"password\":\"pwd\"}"))
        .andReturn().getResponse().getContentAsString(), "$.access_token");

  conversationRepository.save(new ConversationEntity("conv-1","t","p","c-ext","WAITING_USER",null,null,null,Instant.now()));
  messageDraftRepository.save(new MessageDraftEntity("draft-1","conv-1","content","SYSTEM",false,Instant.now()));

  mockMvc.perform(post("/api/drafts/{id}/approve", "draft-1")
      .header("Authorization", bearer)
      .contentType(MediaType.APPLICATION_JSON).content("{\"action\":\"approve\"}"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.draft.approved").value(true))
    .andExpect(jsonPath("$.action_hint.fill_content").exists());

  mockMvc.perform(post("/api/drafts/{id}/reject", "draft-1")
      .header("Authorization", bearer)
      .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"bad\"}"))
    .andExpect(status().isOk());

  mockMvc.perform(post("/api/conversations/{id}/close", "conv-1")
      .header("Authorization", bearer)
      .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"done\"}"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.conversation.status").value("CLOSED"));

  mockMvc.perform(post("/api/conversations/{id}/regenerate", "conv-1")
      .header("Authorization", bearer)
      .contentType(MediaType.APPLICATION_JSON).content("{\"style\":\"short\"}"))
    .andExpect(status().isOk());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `/Users/lushiwu/dev/apache-maven-3.9.4/bin/mvn -Dmaven.repo.local=/Users/lushiwu/Documents/job-agent-mvp/.m2repo test -Dtest=ConversationControllerTest`

Expected: FAIL

- [ ] **Step 3: Implement controllers + service methods**

```java
@GetMapping("/conversations/{id}")
public ConversationDetailResponse detail(@RequestHeader("Authorization") String authHeader,
    @PathVariable String id) {
  authService.verifyAccessToken(authHeader);
  return conversationService.detail(id);
}
```

```java
@PostMapping("/drafts/{id}/approve")
public DraftApproveResponse approve(@RequestHeader("Authorization") String authHeader,
    @PathVariable String id, @RequestBody DraftApproveRequest req) {
  authService.verifyAccessToken(authHeader);
  return conversationService.approveDraft(id);
}

@PostMapping("/drafts/{id}/reject")
public StatusResponse reject(@RequestHeader("Authorization") String authHeader,
    @PathVariable String id, @RequestBody DraftRejectRequest req) {
  authService.verifyAccessToken(authHeader);
  return conversationService.rejectDraft(id);
}

@PostMapping("/conversations/{id}/close")
public ConversationCloseResponse close(@RequestHeader("Authorization") String authHeader,
    @PathVariable String id, @RequestBody CloseRequest req) {
  authService.verifyAccessToken(authHeader);
  return conversationService.close(id);
}

@PostMapping("/conversations/{id}/regenerate")
public DraftRegenerateResponse regenerate(@RequestHeader("Authorization") String authHeader,
    @PathVariable String id, @RequestBody RegenerateRequest req) {
  authService.verifyAccessToken(authHeader);
  return conversationService.regenerate(id);
}
```

```java
// Audit workbench actions
String userId = authService.requireUserId(authHeader);
auditService.record(userId, "DRAFT_APPROVE", mapper.writeValueAsString(req));
auditService.record(userId, "DRAFT_REJECT", mapper.writeValueAsString(req));
auditService.record(userId, "CONVERSATION_CLOSE", mapper.writeValueAsString(req));
auditService.record(userId, "DRAFT_REGENERATE", mapper.writeValueAsString(req));
```

```java
// All workbench endpoints verify Authorization header
authService.verifyAccessToken(authHeader);
```

- [ ] **Step 4: Run test to verify it passes**

Run: `/Users/lushiwu/dev/apache-maven-3.9.4/bin/mvn -Dmaven.repo.local=/Users/lushiwu/Documents/job-agent-mvp/.m2repo test -Dtest=ConversationControllerTest`

Expected: PASS

- [ ] **Step 5: Checkpoint (git disabled)**

Confirm files saved and test output recorded.

---

## Chunk 3: Worker Service (Job Match / Draft / Reply Classify)

### Task 7: Build Worker HTTP API

**Files:**
- Create: `worker/src/job_agent_worker/app.py`
- Create: `worker/src/job_agent_worker/models.py`
- Create: `worker/tests/test_worker_api.py`

- [ ] **Step 1: Write failing test**

```python
import unittest
from job_agent_worker.app import app
from fastapi.testclient import TestClient

class WorkerApiTest(unittest.TestCase):
    def setUp(self):
        self.client = TestClient(app)

    def test_job_match(self):
        resp = self.client.post("/worker/job-match", headers={"X-Worker-Token":"worker-secret"}, json={
            "task_id":"t1","stage":"JOB_MATCH",
            "job_post":{"external_id":"1","source":"zhipin","jd_raw":"产品"},
            "resume":{"content":"产品"},
            "strategy":{"keywords":["产品"]},
            "idempotency_key":"k"
        })
        self.assertEqual(resp.status_code, 200)
        self.assertIn("score", resp.json())

    def test_goal_parse(self):
        resp = self.client.post("/worker/goal-parse", headers={"X-Worker-Token":"worker-secret"}, json={
            "task_id":"t1","stage":"GOAL_PARSE","strategy_text":"只看产品","idempotency_key":"k0"
        })
        self.assertEqual(resp.status_code, 200)
        self.assertIn("strategy_json", resp.json())

    def test_missing_token(self):
        resp = self.client.post("/worker/job-match", json={
            "task_id":"t1","stage":"JOB_MATCH","job_post":{},"resume":{},"strategy":{},"idempotency_key":"k"
        })
        self.assertEqual(resp.status_code, 401)

    def test_invalid_token(self):
        resp = self.client.post("/worker/job-match", headers={"X-Worker-Token":"bad"}, json={
            "task_id":"t1","stage":"JOB_MATCH","job_post":{},"resume":{},"strategy":{},"idempotency_key":"k"
        })
        self.assertEqual(resp.status_code, 401)
```

- [ ] **Step 2: Run test to verify it fails**

Run: `PYTHONPATH=worker/src python3 -m unittest worker/tests/test_worker_api.py`

Expected: FAIL (module not found)

- [ ] **Step 3: Implement FastAPI app**

```python
import json
import os
from fastapi import FastAPI, Header, HTTPException, Depends
from job_agent_worker.models import (
    GoalParseRequest,
    JobMatchRequest,
    DraftRequest,
    ReplyClassifyRequest,
)

app = FastAPI()
_IDEMPOTENCY_CACHE = {}
WORKER_TOKEN = os.getenv("WORKER_TOKEN", "worker-secret")

def verify_token(x_worker_token: str | None = Header(default=None)):
    if not x_worker_token or x_worker_token != WORKER_TOKEN:
        raise HTTPException(status_code=401, detail="WORKER_TOKEN_INVALID")

@app.post("/worker/job-match")
def job_match(req: JobMatchRequest, _: None = Depends(verify_token)):
    if req.idempotency_key in _IDEMPOTENCY_CACHE:
        return _IDEMPOTENCY_CACHE[req.idempotency_key]
    text = (req.job_post.get("jd_raw") or "") + (req.resume.get("content") or "")
    score = 80 if "产品" in text else 60
    resp = {"score": score, "reasons": ["关键词匹配"], "risks": []}
    _IDEMPOTENCY_CACHE[req.idempotency_key] = resp
    return resp

@app.post("/worker/goal-parse")
def goal_parse(req: GoalParseRequest, _: None = Depends(verify_token)):
    if req.idempotency_key in _IDEMPOTENCY_CACHE:
        return _IDEMPOTENCY_CACHE[req.idempotency_key]
    keywords = [word for word in ["产品", "B端"] if word in (req.strategy_text or "")]
    resp = {"strategy_json": {"keywords": keywords, "raw": req.strategy_text}}
    _IDEMPOTENCY_CACHE[req.idempotency_key] = resp
    return resp
```

```python
# worker/src/job_agent_worker/models.py
from pydantic import BaseModel
from typing import Any, Dict, List

class GoalParseRequest(BaseModel):
    task_id: str
    stage: str
    strategy_text: str
    idempotency_key: str

class JobMatchRequest(BaseModel):
    task_id: str
    stage: str
    job_post: Dict[str, Any]
    resume: Dict[str, Any]
    strategy: Dict[str, Any]
    idempotency_key: str

class DraftRequest(BaseModel):
    task_id: str
    stage: str
    conversation: Dict[str, Any]
    job_post: Dict[str, Any]
    resume: Dict[str, Any]
    idempotency_key: str

class ReplyClassifyRequest(BaseModel):
    task_id: str
    stage: str
    conversation: Dict[str, Any]
    messages: List[Dict[str, Any]]
    last_message_id: str
    idempotency_key: str
```

- [ ] **Step 4: Run test to verify it passes**

Run: `PYTHONPATH=worker/src python3 -m unittest worker/tests/test_worker_api.py`

Expected: PASS

- [ ] **Step 5: Checkpoint (git disabled)**

Confirm files saved and test output recorded.

### Task 8: Add Draft + Reply Classify Endpoints

**Files:**
- Modify: `worker/src/job_agent_worker/app.py`
- Modify: `worker/tests/test_worker_api.py`

- [ ] **Step 1: Write failing tests**

```python
    def test_draft(self):
        resp = self.client.post("/worker/draft", headers={"X-Worker-Token":"worker-secret"}, json={
            "task_id":"t1","stage":"DRAFT",
            "conversation":{"external_id":"c1"},
            "job_post":{"title":"产品经理","company":"智聘"},
            "resume":{},
            "idempotency_key":"k2"
        })
        self.assertEqual(resp.status_code, 200)
        self.assertIn("content", resp.json())

    def test_reply_classify(self):
        resp = self.client.post("/worker/reply-classify", headers={"X-Worker-Token":"worker-secret"}, json={
            "task_id":"t1","stage":"REPLY_CLASSIFY",
            "conversation":{"external_id":"c1"},
            "messages":[{"text":"安排面试"}],
            "last_message_id":"m1",
            "idempotency_key":"k3"
        })
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.json()["intent"], "INTERVIEW")
```

- [ ] **Step 2: Run test to verify it fails**

Run: `PYTHONPATH=worker/src python3 -m unittest worker/tests/test_worker_api.py`

Expected: FAIL (endpoints missing)

- [ ] **Step 3: Implement endpoints**

```python
@app.post("/worker/draft")
def draft(req: DraftRequest, _: None = Depends(verify_token)):
    if req.idempotency_key in _IDEMPOTENCY_CACHE:
        return _IDEMPOTENCY_CACHE[req.idempotency_key]
    title = req.job_post.get("title") or "该岗位"
    company = req.job_post.get("company") or "贵司"
    resp = {"content": f"你好，我对{company}的{title}岗位很感兴趣，期待沟通。"}
    _IDEMPOTENCY_CACHE[req.idempotency_key] = resp
    return resp

@app.post("/worker/reply-classify")
def reply_classify(req: ReplyClassifyRequest, _: None = Depends(verify_token)):
    if req.idempotency_key in _IDEMPOTENCY_CACHE:
        return _IDEMPOTENCY_CACHE[req.idempotency_key]
    text = (req.messages[-1].get("text") if req.messages else "")
    if "面试" in text:
        resp = {"intent":"INTERVIEW","summary":"HR 提到面试","next_action":"确认时间"}
    else:
        resp = {"intent":"FOLLOW_UP","summary":"待跟进","next_action":"继续沟通"}
    _IDEMPOTENCY_CACHE[req.idempotency_key] = resp
    return resp
```

- [ ] **Step 4: Run test to verify it passes**

Run: `PYTHONPATH=worker/src python3 -m unittest worker/tests/test_worker_api.py`

Expected: PASS

- [ ] **Step 5: Checkpoint (git disabled)**

Confirm files saved and test output recorded.

---

## Chunk 4: Extension + Web Workbench

### Task 9: Extension Extractor

**Files:**
- Create: `extension/src/extractor.js`
- Modify: `extension/src/content.js`
- Test: `extension/tests/extractor.test.mjs`

- [ ] **Step 1: Write failing test**

```js
import assert from "node:assert/strict";
import { test } from "node:test";
import { detectPageType } from "../src/extractor.js";

test("detectPageType matches detail", () => {
  assert.equal(detectPageType("https://example.com/job_detail/123"), "detail");
});
```


- [ ] **Step 2: Run test to verify it fails**

Run: `node --test extension/tests/extractor.test.mjs`

Expected: FAIL (module not found)

- [ ] **Step 3: Implement extractor + update content script**

```js
export const detectPageType = (url) => {
  if (url.includes("job_detail")) return "detail";
  if (url.includes("chat")) return "chat";
  return "list";
};

export const extractJobPayload = () => {
  const title = document.querySelector("h1")?.textContent?.trim() || document.title;
  const company = document.querySelector("[class*='company']")?.textContent?.trim() || "";
  return { title, company };
};

export const extractChatMessages = () => {
  return Array.from(document.querySelectorAll("[class*='message']"))
    .map((el, idx) => ({ id: String(idx), role: "hr", text: el.textContent?.trim() || "", ts: Date.now() }));
};

export const hashText = (text) => {
  let hash = 0;
  for (let i = 0; i < text.length; i += 1) hash = (hash * 31 + text.charCodeAt(i)) | 0;
  return Math.abs(hash).toString(16);
};

// content.js: send payload to background
const { task_id } = await chrome.storage.local.get("task_id");
const rawText = document.body.innerText || "";
const domHash = hashText(rawText);
const pageType = detectPageType(location.href);
if (pageType === "chat") {
  const messages = extractChatMessages();
  const conversationId = hashText(location.href);
  const lastMessageId = messages.length ? messages[messages.length - 1].id : "";
  chrome.runtime.sendMessage({ type: "CHAT_REPORT", payload: { task_id, conversation_id: conversationId, messages, last_message_id: lastMessageId } });
} else {
  chrome.runtime.sendMessage({
    type: "PAGE_REPORT",
    payload: {
      task_id,
      page_type: pageType,
      raw_text: rawText,
      extracted_json: extractJobPayload(),
      source_url: location.href,
      dom_hash: domHash,
      want_draft: pageType === "detail",
    },
  });
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `node --test extension/tests/extractor.test.mjs`

Expected: PASS

- [ ] **Step 5: Checkpoint (git disabled)**

Confirm files saved and test output recorded.

### Task 10: Extension Popup Login + Token Storage + Reporting

**Files:**
- Create: `extension/src/api.js`
- Modify: `extension/src/popup.html`
- Modify: `extension/src/popup.js`
- Modify: `extension/src/background.js`
- Modify: `extension/src/styles.css`
- Test: `extension/tests/api.test.mjs`

- [ ] **Step 1: Write failing test**

```js
import assert from "node:assert/strict";
import { buildAuthHeaders } from "../src/api.js";
import { test } from "node:test";

test("buildAuthHeaders adds plugin token", () => {
  assert.equal(buildAuthHeaders("token").get("X-Plugin-Token"), "token");
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node --test extension/tests/api.test.mjs`

Expected: FAIL (api.js missing)

- [ ] **Step 3: Implement API helper + popup login**

```js
export const buildAuthHeaders = (pluginToken) => {
  const headers = new Headers({ "Content-Type": "application/json" });
  if (pluginToken) headers.set("X-Plugin-Token", pluginToken);
  return headers;
};

export const API_BASE = "http://localhost:8080";

export const loginAndStoreToken = async (account, password) => {
  const loginResp = await fetch(`${API_BASE}/api/auth/login`, { method: "POST", body: JSON.stringify({ account, password }) });
  const { access_token } = await loginResp.json();
  const pluginResp = await fetch(`${API_BASE}/api/auth/plugin/token`, { method: "POST", body: JSON.stringify({ access_token, browser_id: "chrome" }) });
  const { plugin_token } = await pluginResp.json();
  await chrome.storage.local.set({ plugin_token });
  return plugin_token;
};
```

```js
// background.js: receive page/chat payloads and call plugin APIs
chrome.runtime.onMessage.addListener(async (msg) => {
  if (msg.type === "PAGE_REPORT") await postPageReport(msg.payload);
  if (msg.type === "CHAT_REPORT") await postChatReport(msg.payload);
});

setInterval(async () => {
  const { plugin_token } = await chrome.storage.local.get("plugin_token");
  if (plugin_token) await postHeartbeat({ status: "ok", ts: Date.now() }, plugin_token);
}, 30000);
```

```js
export const postPageReport = async (payload) => {
  const { plugin_token } = await chrome.storage.local.get("plugin_token");
  return fetch(`${API_BASE}/plugin/page/report`, {
    method: "POST",
    headers: buildAuthHeaders(plugin_token),
    body: JSON.stringify(payload),
  });
};

export const postChatReport = async (payload) => {
  const { plugin_token } = await chrome.storage.local.get("plugin_token");
  return fetch(`${API_BASE}/plugin/chat/report`, {
    method: "POST",
    headers: buildAuthHeaders(plugin_token),
    body: JSON.stringify(payload),
  });
};

export const postHeartbeat = async (payload, pluginToken) => {
  return fetch(`${API_BASE}/plugin/heartbeat`, {
    method: "POST",
    headers: buildAuthHeaders(pluginToken),
    body: JSON.stringify(payload),
  });
};
```

- [ ] **Step 4: Run test to verify it passes**

Run: `node --test extension/tests/api.test.mjs`

Expected: PASS

- [ ] **Step 5: Checkpoint (git disabled)**

Confirm files saved and test output recorded.

### Task 11: Extension UI Injection + Draft Fill

**Files:**
- Create: `extension/src/ui.js`
- Modify: `extension/src/content.js`
- Test: `extension/tests/ui.test.mjs`

- [ ] **Step 1: Write failing test**

```js
import { buildOverlayHtml } from "../src/ui.js";
import assert from "node:assert/strict";
import { test } from "node:test";

test("overlay includes score", () => {
  const html = buildOverlayHtml({ score: 80, reasons: ["匹配"], risks: [] });
  assert.ok(html.includes("80"));
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node --test extension/tests/ui.test.mjs`

Expected: FAIL (ui.js missing)

- [ ] **Step 3: Implement overlay builder**

```js
export const buildOverlayHtml = ({ score, reasons, risks, draft }) => {
  return `
    <div class="job-agent-overlay">
      <div class="score">${score}</div>
      <div class="reasons">${reasons.join(" / ")}</div>
      <div class="risks">${risks.join(" / ")}</div>
      <textarea class="draft">${draft?.content ?? ""}</textarea>
      <button data-action="fill">填充</button>
      <button data-action="ignore">忽略</button>
    </div>
  `;
};
```

```js
// content.js: handle overlay actions
overlay.addEventListener("click", (e) => {
  if (e.target.dataset.action === "fill") {
    const input = document.querySelector("textarea, [contenteditable='true']");
    if (input) {
      if (input.tagName === "TEXTAREA") input.value = overlay.querySelector(".draft").value;
      else input.textContent = overlay.querySelector(".draft").value;
    }
  }
  if (e.target.dataset.action === "ignore") {
    overlay.remove();
  }
});
```

- [ ] **Step 4: Run test to verify it passes**

Run: `node --test extension/tests/ui.test.mjs`

Expected: PASS

- [ ] **Step 5: Checkpoint (git disabled)**

Confirm files saved and test output recorded.

### Task 12: Web Workbench Pages + API Clients

**Files:**
- Create: `web/src/app/login/page.tsx`
- Create: `web/src/app/profile/page.tsx`
- Create: `web/src/app/resume/page.tsx`
- Create: `web/src/app/tasks/page.tsx`
- Modify: `web/src/app/page.tsx`
- Modify: `web/src/lib/dashboard.js`
- Modify: `web/src/lib/tasks.js`
- Create: `web/src/lib/auth.js`
- Create: `web/src/lib/resume.js`
- Test: `web/tests/dashboard.test.mjs`
- Test: `web/tests/auth.test.mjs`

- [ ] **Step 1: Write failing test**

```js
import { login } from "../src/lib/auth.js";
import assert from "node:assert/strict";
import { test } from "node:test";

test("login returns tokens on ok response", async () => {
  globalThis.fetch = async () => ({ ok: true, json: async () => ({ access_token: "a" }) });
  const data = await login("http://x", { account: "a", password: "b" });
  assert.equal(data.access_token, "a");
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node --test web/tests/dashboard.test.mjs web/tests/auth.test.mjs`

Expected: FAIL (auth lib missing)

- [ ] **Step 3: Implement libs and pages**

```js
export async function login(baseUrl, payload) {
  const res = await fetch(`${baseUrl}/api/auth/login`, { method: "POST", body: JSON.stringify(payload) });
  if (!res.ok) throw new Error("login failed");
  return res.json();
}
```

```js
// dashboard.js
export const fallbackDashboard = { metrics: { recommendations: 0, drafts: 0, replies: 0, interviews: 0 }, recommendations: [], drafts: [], replies: [], interviews: [] };
export const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";

export async function fetchDashboard(baseUrl = API_BASE, token = "") {
  const res = await fetch(`${baseUrl}/api/dashboard`, { headers: { Authorization: `Bearer ${token}` } });
  return res.ok ? res.json() : fallbackDashboard;
}
```

```tsx
// page.tsx: client component to access localStorage
"use client";
import { useEffect, useState } from "react";
import { fetchDashboard, fallbackDashboard } from "../lib/dashboard.js";

export default function Home() {
  const [dashboard, setDashboard] = useState(fallbackDashboard);
  useEffect(() => {
    const token = localStorage.getItem("access_token") ?? "";
    fetchDashboard(undefined, token).then(setDashboard);
  }, []);
  return (
    <main>
      <section>{dashboard.recommendations.map((item) => <div key={item.job_post_id}>{item.title}</div>)}</section>
      <section>{dashboard.drafts.map((item) => <div key={item.draft_id}>{item.content}</div>)}</section>
      <section>{dashboard.replies.map((item) => <div key={item.conversation_id}>{item.summary}</div>)}</section>
      <section>{dashboard.interviews.map((item) => <div key={item.conversation_id}>{item.title}</div>)}</section>
    </main>
  );
}
```

```tsx
// login/page.tsx: "use client" form -> POST /api/auth/login, store access_token in localStorage
// resume/page.tsx: textarea + submit -> POST /api/resume
// tasks/page.tsx: form fields -> POST /api/tasks, list existing tasks
// profile/page.tsx: render account info placeholder
```

- [ ] **Step 4: Run test to verify it passes**

Run: `node --test web/tests/dashboard.test.mjs web/tests/auth.test.mjs`

Expected: PASS

- [ ] **Step 5: Checkpoint (git disabled)**

Confirm files saved and test output recorded.

---

Plan complete and saved to `docs/superpowers/plans/2026-03-14-agent-full-plan.md`. Ready to execute?
