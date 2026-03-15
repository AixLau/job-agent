package com.jobagent.server.repository;

import com.jobagent.server.store.ResumeEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ResumeRepositoryTest {

    @Autowired
    private ResumeRepository resumeRepository;

    @Test
    void saveAndLoadResume() {
        ResumeEntity entity = new ResumeEntity(
            "r-1",
            "user-1",
            "张三\n产品经理",
            "{\"name\":\"张三\"}"
        );

        resumeRepository.save(entity);

        ResumeEntity loaded = resumeRepository.findById("r-1").orElseThrow();
        assertThat(loaded.getContent()).contains("张三");
        assertThat(loaded.getParsedJson()).contains("name");
    }
}
