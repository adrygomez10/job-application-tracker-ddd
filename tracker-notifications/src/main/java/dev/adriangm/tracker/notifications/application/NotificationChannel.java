package dev.adriangm.tracker.notifications.application;

import dev.adriangm.tracker.notifications.api.NotificationMessage;
import dev.adriangm.tracker.notifications.domain.DeliveryState;

public interface NotificationChannel {
    boolean configured();

    DeliveryState send(NotificationMessage message);
}
