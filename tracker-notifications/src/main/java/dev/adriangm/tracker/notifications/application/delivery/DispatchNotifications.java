package dev.adriangm.tracker.notifications.application.delivery;

import dev.adriangm.tracker.notifications.api.NotificationMessage;
import dev.adriangm.tracker.notifications.api.NotificationPreferences;
import dev.adriangm.tracker.notifications.application.port.NotificationChannel;
import dev.adriangm.tracker.notifications.application.port.NotificationQueue;
import dev.adriangm.tracker.notifications.domain.DeliveryState;
import dev.adriangm.tracker.notifications.domain.QuietHours;
import dev.adriangm.tracker.reminders.api.ReminderNotifications;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;

/** A pending claim is committed before HTTP. An ambiguous attempt is never reclaimed. */
public final class DispatchNotifications {
    private final NotificationQueue queue;
    private final NotificationPreferences preferences;
    private final ReminderNotifications reminders;
    private final NotificationChannel channel;
    private final Clock clock;

    public DispatchNotifications(
        NotificationQueue queue,
        NotificationPreferences preferences,
        ReminderNotifications reminders,
        NotificationChannel channel,
        Clock clock
    )
    {
        this.queue = queue;
        this.preferences = preferences;
        this.reminders = reminders;
        this.channel = channel;
        this.clock = clock;
    }

    public void execute()
    {
        for (var entry : queue.pending(25)) {
            dispatch(entry);
        }
    }

    private void dispatch(NotificationQueue.Entry entry)
    {
        var message = entry.message();
        var settings = preferences.current();
        var now = clock.instant();

        var terminal = terminalState(message, settings, now);
        if (terminal != null) {
            queue.dismiss(message.key(), terminal, now);
            return;
        }

        var quiet = new QuietHours(settings.quietFrom(), settings.quietUntil());
        if (!channel.configured() || quiet.includes(LocalTime.ofInstant(now, settings.zone()))) {
            return;
        }

        if (queue.claim(message.key(), now)) {
            var result = channel.send(message);
            queue.finish(message.key(), result, clock.instant());
        }
    }

    private DeliveryState terminalState(
        NotificationMessage message,
        NotificationPreferences.Preferences settings,
        Instant now
    )
    {
        if (revoked(message, settings) || inactive(message)) {
            return DeliveryState.CANCELLED;
        }
        if (message.expiresAt() != null && !now.isBefore(message.expiresAt())) {
            return DeliveryState.EXPIRED;
        }
        return null;
    }

    private static boolean revoked(NotificationMessage message, NotificationPreferences.Preferences settings)
    {
        boolean automaticDisabled = !settings.automatic() || !settings.permits(message.kind());
        return !settings.enabled() || (message.automatic() && automaticDisabled);
    }

    private boolean inactive(NotificationMessage message)
    {
        return message.activityId() != null && reminders
            .active()
            .stream()
            .noneMatch(
                item -> item.activityId().equals(message.activityId()) && item.kind().equals(message.kind())
                    && item.dueAt().equals(message.dueAt())
            );
    }
}
