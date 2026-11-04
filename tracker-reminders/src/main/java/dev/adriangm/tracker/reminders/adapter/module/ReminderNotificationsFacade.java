package dev.adriangm.tracker.reminders.adapter.module;

import dev.adriangm.tracker.applications.api.ApplicationsApi;
import dev.adriangm.tracker.reminders.api.ReminderNotifications;
import dev.adriangm.tracker.reminders.application.ReminderPolicy;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ReminderNotificationsFacade implements ReminderNotifications {
    private final ApplicationsApi applications;
    private final ReminderPolicy policy;

    public ReminderNotificationsFacade(ApplicationsApi applications, Clock clock)
    {
        this.applications = applications;
        this.policy = new ReminderPolicy(clock);
    }

    @Override
    public List<DueReminder> active()
    {
        return policy
            .due(applications.openActivities())
            .stream()
            .map(item -> new DueReminder(item.activityId(), item.kind(), item.dueAt()))
            .toList();
    }
}
