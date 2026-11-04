package dev.adriangm.tracker.search.domain;

public record AutomationSettings(
    boolean importEnabled,
    int importIntervalMinutes,
    boolean notificationsEnabled,
    boolean tasks,
    boolean interviews,
    boolean newOpportunities
) {
    public AutomationSettings
    {
        if (importIntervalMinutes < 15 || importIntervalMinutes > 1440) {
            throw new IllegalArgumentException("Import interval must be between 15 and 1440 minutes");
        }
    }

    public static AutomationSettings defaults()
    {
        return new AutomationSettings(false, 60, false, true, true, true);
    }
}
