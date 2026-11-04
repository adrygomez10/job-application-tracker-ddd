package dev.adriangm.tracker.notifications.application;

import dev.adriangm.tracker.notifications.api.NotificationMessage;
import dev.adriangm.tracker.notifications.domain.DeliveryState;
import java.time.Instant;
import java.util.List;

public interface NotificationQueue {
    boolean enqueue(NotificationMessage message);

    List<Entry> pending(int limit);

    boolean claim(String key, Instant now);

    void finish(String key, DeliveryState state, Instant now);

    void dismiss(String key, DeliveryState state, Instant now);

    void recover(Instant now);

    List<Entry> history(int page);

    record Entry(NotificationMessage message, DeliveryState state, Instant updatedAt) {
    }
}
