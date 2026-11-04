package dev.adriangm.tracker.reminders.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ReminderNotifications {
    List<DueReminder> active();

    record DueReminder(UUID activityId, String kind, Instant dueAt) {
    }
}
