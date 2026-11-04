package dev.adriangm.tracker.applications.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ActiveProcessPolicyTest {
    @Test
    void aClosedProcessDoesNotConsumeAnActiveSlot()
    {
        assertThatCode(() -> new ActiveProcessPolicy().requireAvailable(0)).doesNotThrowAnyException();
        assertThatThrownBy(() -> new ActiveProcessPolicy().requireAvailable(1))
            .isInstanceOf(RuleViolation.class)
            .hasMessageContaining("activeOpportunity");
    }
}
