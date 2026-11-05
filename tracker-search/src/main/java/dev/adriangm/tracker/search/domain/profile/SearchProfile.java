package dev.adriangm.tracker.search.domain.profile;

import dev.adriangm.tracker.search.domain.SalaryRange;
import dev.adriangm.tracker.search.domain.WorkMode;
import dev.adriangm.tracker.search.domain.validation.Text;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;

/** One local owner's preferences. Preferences never change recorded process facts. */
public record SearchProfile(
    long version,
    String displayName,
    String targetRole,
    String location,
    WorkMode preferredMode,
    SalaryRange targetSalary,
    ZoneId zone,
    boolean externalNotifications,
    LocalTime quietFrom,
    LocalTime quietUntil,
    AutomationSettings automation
) {
    public SearchProfile
    {
        displayName = Text.required(displayName, 120);
        targetRole = Text.optional(targetRole, 180);
        location = Text.optional(location, 160);
        Objects.requireNonNull(preferredMode);
        Objects.requireNonNull(zone);
        Objects.requireNonNull(automation);
        Objects.requireNonNull(quietFrom);
        Objects.requireNonNull(quietUntil);
        if (version < 0) {
            throw new IllegalArgumentException("Invalid profile version");
        }
    }
}
