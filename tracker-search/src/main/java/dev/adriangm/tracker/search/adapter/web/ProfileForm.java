package dev.adriangm.tracker.search.adapter.web;

import dev.adriangm.tracker.search.domain.SearchProfile;
import dev.adriangm.tracker.search.domain.WorkMode;
import java.time.LocalTime;
import java.time.ZoneId;

public record ProfileForm(
    Long version,
    String displayName,
    String targetRole,
    String location,
    WorkMode preferredMode,
    SalaryForm salary,
    String zone,
    Boolean externalNotifications,
    @org.springframework.format.annotation.DateTimeFormat(
        iso = org.springframework.format.annotation.DateTimeFormat.ISO.TIME
    ) LocalTime quietFrom,
    @org.springframework.format.annotation.DateTimeFormat(
        iso = org.springframework.format.annotation.DateTimeFormat.ISO.TIME
    ) LocalTime quietUntil,
    Boolean autoImport,
    Integer importInterval,
    Boolean autoNotifications,
    Boolean notifyTasks,
    Boolean notifyInterviews,
    Boolean notifyOpportunities
) {
    public SearchProfile profile()
    {
        if (zone == null || zone.isBlank()) {
            throw new IllegalArgumentException("Zone is required");
        }
        if (version == null || importInterval == null) {
            throw new IllegalArgumentException("Profile version and interval are required");
        }
        return new SearchProfile(
            version,
            displayName,
            targetRole,
            location,
            preferredMode,
            salary.toDomain(),
            ZoneId.of(zone),
            Boolean.TRUE.equals(externalNotifications),
            quietFrom,
            quietUntil,
            new dev.adriangm.tracker.search.domain.AutomationSettings(
                Boolean.TRUE.equals(autoImport),
                importInterval,
                Boolean.TRUE.equals(autoNotifications),
                Boolean.TRUE.equals(notifyTasks),
                Boolean.TRUE.equals(notifyInterviews),
                Boolean.TRUE.equals(notifyOpportunities)
            )
        );
    }
}
