package com.jobagent.server.repository;

import com.jobagent.server.store.TaskEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void saveAndLoadTask() {
        TaskEntity entity = new TaskEntity(
            "t-1",
            "产品经理",
            "上海",
            "20k-30k",
            "5年",
            "SEMI_AUTO",
            "ACTIVE"
        );

        taskRepository.save(entity);

        TaskEntity loaded = taskRepository.findById("t-1").orElseThrow();
        assertThat(loaded.getTargetRole()).isEqualTo("产品经理");
        assertThat(loaded.getCity()).isEqualTo("上海");
        assertThat(loaded.getStatus()).isEqualTo("ACTIVE");
    }
}
