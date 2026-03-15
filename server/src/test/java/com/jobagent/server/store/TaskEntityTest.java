package com.jobagent.server.store;

import jakarta.persistence.Lob;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class TaskEntityTest {

    @Test
    void json_fields_are_lob() throws Exception {
        assertThat(hasLob("strategyJson")).isTrue();
        assertThat(hasLob("ruleConfigJson")).isTrue();
        assertThat(hasLob("excludeJson")).isTrue();
        assertThat(hasLob("preferencesJson")).isTrue();
    }

    private boolean hasLob(String fieldName) throws Exception {
        Field field = TaskEntity.class.getDeclaredField(fieldName);
        return field.isAnnotationPresent(Lob.class);
    }
}
