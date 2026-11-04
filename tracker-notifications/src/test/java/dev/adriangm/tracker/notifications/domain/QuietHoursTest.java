package dev.adriangm.tracker.notifications.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class QuietHoursTest {
    @Test
    void nightIntervalIncludesStartAndExcludesEnd()
    {
        var hours = new QuietHours(LocalTime.of(22, 0), LocalTime.of(8, 0));
        assertThat(hours.includes(LocalTime.of(22, 0))).isTrue();
        assertThat(hours.includes(LocalTime.MIDNIGHT)).isTrue();
        assertThat(hours.includes(LocalTime.of(7, 59))).isTrue();
        assertThat(hours.includes(LocalTime.of(8, 0))).isFalse();
        assertThat(hours.includes(LocalTime.NOON)).isFalse();
    }

    @Test
    void daytimeIntervalAndEqualEndpointsHaveExplicitMeaning()
    {
        var hours = new QuietHours(LocalTime.of(12, 0), LocalTime.of(14, 0));
        assertThat(hours.includes(LocalTime.of(13, 0))).isTrue();
        assertThat(hours.includes(LocalTime.of(14, 0))).isFalse();
        assertThat(new QuietHours(LocalTime.NOON, LocalTime.NOON).includes(LocalTime.NOON)).isFalse();
    }
}
