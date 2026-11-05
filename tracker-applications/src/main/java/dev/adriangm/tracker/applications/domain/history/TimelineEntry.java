package dev.adriangm.tracker.applications.domain.history;

import dev.adriangm.tracker.applications.domain.status.Status;
import dev.adriangm.tracker.applications.domain.validation.Checks;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TimelineEntry(
    UUID id,
    Instant occurredAt,
    TimelineEvent kind,
    Status previousStatus,
    Status nextStatus,
    String body
) {
    public TimelineEntry
    {
        Objects.requireNonNull(id);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(kind);
        body = Checks.optionalText(body, 10000);
    }

    public static TimelineEntry of(Instant now, TimelineEvent kind, String body)
    {
        return new TimelineEntry(UUID.randomUUID(), now, kind, null, null, body);
    }

    public static TimelineEntry statusChange(
        Instant now,
        TimelineEvent kind,
        Status previous,
        Status next,
        String reason
    )
    {
        return new TimelineEntry(UUID.randomUUID(), now, kind, previous, next, reason);
    }
}
