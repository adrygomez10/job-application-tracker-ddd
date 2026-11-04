package dev.adriangm.tracker.notifications.api;

import java.time.Instant;
import java.util.UUID;

/** Enqueue only; implementations never perform network calls. Joins the caller's transaction. */
public interface NotificationRequests {
    void newOpportunities(UUID importId, int count, Instant now);
}
