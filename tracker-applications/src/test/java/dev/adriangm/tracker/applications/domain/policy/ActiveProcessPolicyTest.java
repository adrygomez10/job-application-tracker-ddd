package dev.adriangm.tracker.applications.domain.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.adriangm.tracker.applications.domain.RuleViolation;
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
