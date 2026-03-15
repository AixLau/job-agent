package com.jobagent.server.service;

import jakarta.validation.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelOutputValidatorTest {

    private final ModelOutputValidator validator = new ModelOutputValidator();

    @Test
    void validate_draft_rejects_too_short() {
        assertThatThrownBy(() -> validator.validateDraft("short"))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void validate_draft_rejects_contact_info() {
        String text = "contact at test@example.com";
        assertThatThrownBy(() -> validator.validateDraft(text))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void validate_draft_rejects_contact_info_case_insensitive() {
        String text = "please add me on qq 123";
        assertThatThrownBy(() -> validator.validateDraft(text))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void validate_summary_rejects_too_long() {
        String longText = "a".repeat(201);
        assertThatThrownBy(() -> validator.validateSummary(longText))
            .isInstanceOf(ValidationException.class);
    }
}
