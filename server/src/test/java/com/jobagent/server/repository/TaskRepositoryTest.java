package com.jobagent.server.repository;

import com.jobagent.server.store.TaskEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void saveAndLoadTask() {
        TaskEntity entity = new TaskEntity(
            "t-1",
            "user-1",
            "产品经理",
            "上海",
            "20k-30k",
            "5年",
            "SEMI_AUTO",
            "ACTIVE",
            "{\"goal\":\"test\"}",
            "{}",
            "[]",
            "[]",
            Instant.parse("2026-03-15T00:00:00Z")
        );

        taskRepository.save(entity);

        TaskEntity loaded = taskRepository.findById("t-1").orElseThrow();
        assertThat(loaded.getTitle()).isEqualTo("产品经理");
        assertThat(loaded.getCity()).isEqualTo("上海");
        assertThat(loaded.getStatus()).isEqualTo("ACTIVE");
    }
}
