package dev.adriangm.tracker.notifications.api;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record NotificationMessage(
    String key,
    String kind,
    UUID activityId,
    Instant dueAt,
    int newCount,
    boolean automatic,
    Instant createdAt,
    Instant expiresAt
) {
    public NotificationMessage
    {
        Objects.requireNonNull(key);
        Objects.requireNonNull(kind);
        Objects.requireNonNull(createdAt);
        if (newCount < 0) {
            throw new IllegalArgumentException("Invalid opportunity count");
        }
    }

    public static NotificationMessage reminder(
        UUID activityId,
        String kind,
        Instant dueAt,
        boolean automatic,
        Instant now
    )
    {
        return new NotificationMessage(
            "reminder:" + activityId + ":" + kind + ":" + dueAt,
            kind,
            activityId,
            dueAt,
            0,
            automatic,
            now,
            null
        );
    }

    public static NotificationMessage summary(UUID importId, int count, Instant now)
    {
        return new NotificationMessage(
            "import:" + importId,
            "NEW_OPPORTUNITIES",
            null,
            null,
            count,
            true,
            now,
            now.plusSeconds(86400)
        );
    }

    public String text()
    {
        return kind.equals("NEW_OPPORTUNITIES")
            ? "Job Tracker found " + newCount + " new opportunities."
            : "You have a pending reminder in Job Tracker.";
    }
}
