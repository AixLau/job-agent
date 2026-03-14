# Dashboard Persistence Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist dashboard recommendations, drafts, and replies to the database while keeping existing API responses unchanged.

**Architecture:** Add three JPA entities and repositories for dashboard items, swap `DashboardStore` to read/write via JPA, and keep `DashboardController`/`PluginGatewayController` contracts intact. Data is retrieved per list (not globally merged) with a configurable max size.

**Tech Stack:** Spring Boot 3.3, Spring Data JPA, H2 (test), PostgreSQL (runtime)

---

## File Structure

- Create: `server/src/main/java/com/jobagent/server/store/DashboardRecommendationEntity.java`
- Create: `server/src/main/java/com/jobagent/server/store/DashboardDraftEntity.java`
- Create: `server/src/main/java/com/jobagent/server/store/DashboardReplyEntity.java`
- Create: `server/src/main/java/com/jobagent/server/repository/DashboardRecommendationRepository.java`
- Create: `server/src/main/java/com/jobagent/server/repository/DashboardDraftRepository.java`
- Create: `server/src/main/java/com/jobagent/server/repository/DashboardReplyRepository.java`
- Create: `server/src/test/java/com/jobagent/server/repository/DashboardRepositoryTest.java`
- Create: `server/src/test/java/com/jobagent/server/store/DashboardStoreTest.java`
- Modify: `server/src/main/java/com/jobagent/server/store/DashboardStore.java`
- Modify: `server/src/main/resources/application.yml`

---

## Chunk 1: Persistence Model + Repository Tests

### Task 1: Add Dashboard Entities

**Files:**
- Create: `server/src/main/java/com/jobagent/server/store/DashboardRecommendationEntity.java`
- Create: `server/src/main/java/com/jobagent/server/store/DashboardDraftEntity.java`
- Create: `server/src/main/java/com/jobagent/server/store/DashboardReplyEntity.java`

- [ ] **Step 1: Write failing entity test (red)**

Create `server/src/test/java/com/jobagent/server/repository/DashboardRepositoryTest.java` with one test that persists each entity and reads it back.

```java
package com.jobagent.server.repository;

import com.jobagent.server.store.DashboardDraftEntity;
import com.jobagent.server.store.DashboardRecommendationEntity;
import com.jobagent.server.store.DashboardReplyEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DashboardRepositoryTest {

    @Autowired
    private DashboardRecommendationRepository recommendationRepository;

    @Autowired
    private DashboardDraftRepository draftRepository;

    @Autowired
    private DashboardReplyRepository replyRepository;

    @Test
    void saveAndLoadAllEntities() {
        DashboardRecommendationEntity rec = new DashboardRecommendationEntity(
            "rec-1",
            "资深产品经理",
            "智聘科技",
            88,
            "[\"岗位匹配\"]"
        );
        DashboardDraftEntity draft = new DashboardDraftEntity(
            "draft-1",
            "智聘科技",
            "资深产品经理",
            "您好，我对贵司岗位很感兴趣"
        );
        DashboardReplyEntity reply = new DashboardReplyEntity(
            "reply-1",
            "智聘科技",
            "INTERVIEW",
            "可以安排面试吗",
            "确认面试时间"
        );

        recommendationRepository.save(rec);
        draftRepository.save(draft);
        replyRepository.save(reply);

        DashboardRecommendationEntity recLoaded = recommendationRepository.findById("rec-1").orElseThrow();
        DashboardDraftEntity draftLoaded = draftRepository.findById("draft-1").orElseThrow();
        DashboardReplyEntity replyLoaded = replyRepository.findById("reply-1").orElseThrow();

        assertThat(recLoaded.getCompany()).isEqualTo("智聘科技");
        assertThat(draftLoaded.getContent()).contains("感兴趣");
        assertThat(replyLoaded.getIntent()).isEqualTo("INTERVIEW");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd server && /opt/homebrew/bin/mvn -Dmaven.repo.local=/Users/lushiwu/Documents/job-agent-mvp/.m2repo -Dtest=DashboardRepositoryTest test`

Expected: compilation errors for missing entities/repositories.

- [ ] **Step 3: Implement entities (green)**

Each entity should:
- `@Entity` + `@Table(name = ...)`
- `id` (String) as `@Id`
- required fields as `@Column(nullable = false)`
- `createdAt` (Instant) with `@PrePersist` to set default
- `content`/`summary`/`reasonsJson` use `columnDefinition = "text"`

Example structure (apply per entity):

```java
@Entity
@Table(name = "dashboard_recommendations")
public class DashboardRecommendationEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false, columnDefinition = "text")
    private String reasonsJson;

    @Column(nullable = false)
    private Instant createdAt;

    protected DashboardRecommendationEntity() {}

    public DashboardRecommendationEntity(String id, String title, String company, int score, String reasonsJson) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.score = score;
        this.reasonsJson = reasonsJson;
        this.createdAt = Instant.now();
    }

    @PrePersist
    void ensureCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    // getters
}
```

- [ ] **Step 4: Add repositories**

Create three repositories with `JpaRepository`:

```java
public interface DashboardRecommendationRepository extends JpaRepository<DashboardRecommendationEntity, String> {}
```

Also add query method per repository:

```java
List<DashboardRecommendationEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd server && /opt/homebrew/bin/mvn -Dmaven.repo.local=/Users/lushiwu/Documents/job-agent-mvp/.m2repo -Dtest=DashboardRepositoryTest test`

Expected: 1 test, 0 failures.

- [ ] **Step 6: Commit**

```bash
git add server/src/main/java/com/jobagent/server/store/DashboardRecommendationEntity.java \
  server/src/main/java/com/jobagent/server/store/DashboardDraftEntity.java \
  server/src/main/java/com/jobagent/server/store/DashboardReplyEntity.java \
  server/src/main/java/com/jobagent/server/repository/DashboardRecommendationRepository.java \
  server/src/main/java/com/jobagent/server/repository/DashboardDraftRepository.java \
  server/src/main/java/com/jobagent/server/repository/DashboardReplyRepository.java \
  server/src/test/java/com/jobagent/server/repository/DashboardRepositoryTest.java

git commit -m "新增Dashboard持久化实体"
```

---

## Chunk 2: DashboardStore Persistence + Tests

### Task 2: Persist DashboardStore

**Files:**
- Create: `server/src/test/java/com/jobagent/server/store/DashboardStoreTest.java`
- Modify: `server/src/main/java/com/jobagent/server/store/DashboardStore.java`
- Modify: `server/src/main/resources/application.yml`

- [ ] **Step 1: Write failing DashboardStore test (red)**

Create `server/src/test/java/com/jobagent/server/store/DashboardStoreTest.java`:

```java
package com.jobagent.server.store;

import com.jobagent.server.dto.DraftItem;
import com.jobagent.server.dto.RecommendationItem;
import com.jobagent.server.dto.ReplyItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(DashboardStore.class)
class DashboardStoreTest {

    @Autowired
    private DashboardStore store;

    @Test
    void snapshotReturnsLatestAndMetrics() {
        store.addRecommendation(new RecommendationItem("A", "C1", 80, List.of("r1")));
        store.addDraft(new DraftItem("C1", "A", "d1"));
        store.addReply(new ReplyItem("C1", "INTERVIEW", "s1", "n1"));

        var snapshot = store.snapshot();

        assertThat(snapshot.metrics().recommendations()).isEqualTo(1);
        assertThat(snapshot.metrics().drafts()).isEqualTo(1);
        assertThat(snapshot.metrics().replies()).isEqualTo(1);
        assertThat(snapshot.metrics().interviews()).isEqualTo(1);
        assertThat(snapshot.recommendations().get(0).title()).isEqualTo("A");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd server && /opt/homebrew/bin/mvn -Dmaven.repo.local=/Users/lushiwu/Documents/job-agent-mvp/.m2repo -Dtest=DashboardStoreTest test`

Expected: wiring errors (DashboardStore still uses in-memory collections).

- [ ] **Step 3: Update DashboardStore to JPA**

Update `DashboardStore`:
- Inject repositories + `ObjectMapper`
- Read config `job-agent.dashboard.max-items` via `@Value`
- Replace in-memory deques
- Serialize reasons list to JSON (`[]` on failure)
- Read reasons JSON back to List<String> (`List.of()` on failure)
- Write `RecommendationItem/DraftItem/ReplyItem` to DB with UUID + timestamps
- `snapshot()` uses `PageRequest.of(0, maxItems, Sort.by("createdAt").descending())`
- Keep `clear()` for tests: delete all rows in the three repositories

Pseudo-structure:

```java
@Component
public class DashboardStore {
    private final DashboardRecommendationRepository recommendationRepository;
    private final DashboardDraftRepository draftRepository;
    private final DashboardReplyRepository replyRepository;
    private final ObjectMapper objectMapper;
    private final int maxItems;

    public DashboardStore(..., @Value("${job-agent.dashboard.max-items:20}") int maxItems) {
        ...
    }

    public void addRecommendation(RecommendationItem item) {
        String reasonsJson = writeReasons(item.reasons());
        recommendationRepository.save(new DashboardRecommendationEntity(UUID.randomUUID().toString(), ...));
    }

    public DashboardResponse snapshot() {
        Pageable page = PageRequest.of(0, maxItems, Sort.by("createdAt").descending());
        var recs = recommendationRepository.findAllByOrderByCreatedAtDesc(page).stream()...
        var drafts = draftRepository.findAllByOrderByCreatedAtDesc(page).stream()...
        var replies = replyRepository.findAllByOrderByCreatedAtDesc(page).stream()...

        int interviews = (int) replies.stream().filter(r -> "INTERVIEW".equalsIgnoreCase(r.intent())).count();
        return new DashboardResponse(new DashboardMetrics(recs.size(), drafts.size(), replies.size(), interviews), recs, drafts, replies);
    }

    public void clear() {
        replyRepository.deleteAll();
        draftRepository.deleteAll();
        recommendationRepository.deleteAll();
    }
}
```

Error handling: JSON exceptions should return empty list/"[]". Database writes should be best-effort (catch runtime exceptions and log, do not throw).

- [ ] **Step 4: Update application.yml**

Add config:

```yaml
job-agent:
  dashboard:
    max-items: 20
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd server && /opt/homebrew/bin/mvn -Dmaven.repo.local=/Users/lushiwu/Documents/job-agent-mvp/.m2repo -Dtest=DashboardStoreTest test`

Expected: 1 test, 0 failures.

- [ ] **Step 6: Run impacted test suites**

Run:
- `cd server && /opt/homebrew/bin/mvn -Dmaven.repo.local=/Users/lushiwu/Documents/job-agent-mvp/.m2repo test`

- [ ] **Step 7: Commit**

```bash
git add server/src/main/java/com/jobagent/server/store/DashboardStore.java \
  server/src/main/resources/application.yml \
  server/src/test/java/com/jobagent/server/store/DashboardStoreTest.java

git commit -m "DashboardStore切换为持久化"
```

---

## Chunk 3: Full Regression Check

### Task 3: Full Project Smoke Tests

**Files:** none

- [ ] **Step 1: Run worker tests**

Run: `cd worker && PYTHONPATH=src python3 -m unittest tests/test_graphs.py`

Expected: 3 tests, 0 failures.

- [ ] **Step 2: Run extension tests**

Run: `node --test extension/tests/manifest.test.mjs`

Expected: 1 test, 0 failures.

- [ ] **Step 3: Run web tests**

Run: `node --test web/tests/dashboard.test.mjs web/tests/tasks.test.mjs`

Expected: 3 tests, 0 failures.

- [ ] **Step 4: Commit (if any new changes)**

Only if additional changes were required from these tests.

---

**Execution Ready.**
