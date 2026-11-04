package dev.adriangm.tracker.notifications.application;

import dev.adriangm.tracker.notifications.api.NotificationMessage;
import dev.adriangm.tracker.notifications.api.NotificationPreferences;
import dev.adriangm.tracker.reminders.api.ReminderNotifications;
import java.time.Clock;

public final class CollectNotifications {
    private final NotificationPreferences preferences;
    private final ReminderNotifications reminders;
    private final NotificationQueue queue;
    private final Clock clock;

    public CollectNotifications(
        NotificationPreferences preferences,
        ReminderNotifications reminders,
        NotificationQueue queue,
        Clock clock
    )
    {
        this.preferences = preferences;
        this.reminders = reminders;
        this.queue = queue;
        this.clock = clock;
    }

    public void execute()
    {
        var settings = preferences.current();
        if (!settings.enabled() || !settings.automatic()) {
            return;
        }
        for (var reminder : reminders.active()) {
            if (settings.permits(reminder.kind())) {
                queue
                    .enqueue(
                        NotificationMessage
                            .reminder(reminder.activityId(), reminder.kind(), reminder.dueAt(), true, clock.instant())
                    );
            }
        }
    }
}
