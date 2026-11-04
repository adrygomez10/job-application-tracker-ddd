package dev.adriangm.tracker.notifications.domain;

import java.time.LocalTime;

public record QuietHours(LocalTime from, LocalTime until) {
    /** Equal endpoints disable quiet hours. Start is inclusive; end is exclusive. */
    public boolean includes(LocalTime time)
    {
        if (from.equals(until)) {
            return false;
        }
        if (from.isBefore(until)) {
            return !time.isBefore(from) && time.isBefore(until);
        }
        return !time.isBefore(from) || time.isBefore(until);
    }
}
