package dev.adriangm.tracker.notifications.application.collection;

import dev.adriangm.tracker.notifications.api.NotificationMessage;
import dev.adriangm.tracker.notifications.api.NotificationPreferences;
import dev.adriangm.tracker.notifications.application.port.NotificationChannel;
import dev.adriangm.tracker.notifications.application.port.NotificationQueue;
import dev.adriangm.tracker.reminders.api.ReminderNotifications;
import java.time.Clock;
import java.util.UUID;

/** Explicit owner request; scheduled delivery still respects consent and quiet hours. */
public final class SendReminder {
    public enum Result {
        DISABLED,
        ALREADY_ATTEMPTED,
        QUEUED
    }

    private final NotificationPreferences preferences;
    private final ReminderNotifications reminders;
    private final NotificationQueue queue;
    private final NotificationChannel channel;
    private final Clock clock;

    public SendReminder(
        NotificationPreferences preferences,
        ReminderNotifications reminders,
        NotificationQueue queue,
        NotificationChannel channel,
        Clock clock
    )
    {
        this.preferences = preferences;
        this.reminders = reminders;
        this.queue = queue;
        this.channel = channel;
        this.clock = clock;
    }

    public Result execute(UUID activityId)
    {
        if (!preferences.current().enabled() || !channel.configured()) {
            return Result.DISABLED;
        }
        var reminder = reminders
            .active()
            .stream()
            .filter(item -> item.activityId().equals(activityId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Reminder is no longer active"));
        boolean added = queue
            .enqueue(
                NotificationMessage.reminder(activityId, reminder.kind(), reminder.dueAt(), false, clock.instant())
            );
        return added ? Result.QUEUED : Result.ALREADY_ATTEMPTED;
    }
}
