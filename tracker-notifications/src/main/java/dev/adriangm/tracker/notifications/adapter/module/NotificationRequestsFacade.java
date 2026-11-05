package dev.adriangm.tracker.notifications.adapter.module;

import dev.adriangm.tracker.notifications.api.NotificationMessage;
import dev.adriangm.tracker.notifications.api.NotificationPreferences;
import dev.adriangm.tracker.notifications.api.NotificationRequests;
import dev.adriangm.tracker.notifications.application.port.NotificationQueue;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class NotificationRequestsFacade implements NotificationRequests {
    private final NotificationQueue queue;
    private final NotificationPreferences preferences;

    public NotificationRequestsFacade(NotificationQueue queue, NotificationPreferences preferences)
    {
        this.queue = queue;
        this.preferences = preferences;
    }

    @Override
    public void newOpportunities(UUID importId, int count, Instant now)
    {
        var settings = preferences.current();
        if (count > 0 && settings.enabled() && settings.automatic() && settings.newOpportunities()) {
            queue.enqueue(NotificationMessage.summary(importId, count, now));
        }
    }
}
