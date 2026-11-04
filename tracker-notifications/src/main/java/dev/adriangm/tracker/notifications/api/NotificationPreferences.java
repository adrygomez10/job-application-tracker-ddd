package dev.adriangm.tracker.notifications.api;

import java.time.LocalTime;
import java.time.ZoneId;

/** Minimal profile translation required by the notification context. */
public interface NotificationPreferences {
    Preferences current();

    record Preferences(
        boolean enabled,
        ZoneId zone,
        LocalTime quietFrom,
        LocalTime quietUntil,
        boolean automatic,
        boolean tasks,
        boolean interviews,
        boolean newOpportunities
    ) {
        public boolean permits(String kind)
        {
            return switch (kind) {
                case "TASK" -> tasks;
                case "INTERVIEW" -> interviews;
                case "NEW_OPPORTUNITIES" -> newOpportunities;
                default -> false;
            };
        }
    }
}
