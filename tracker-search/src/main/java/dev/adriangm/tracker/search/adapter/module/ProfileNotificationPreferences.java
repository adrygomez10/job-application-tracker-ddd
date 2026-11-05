package dev.adriangm.tracker.search.adapter.module;

import dev.adriangm.tracker.notifications.api.NotificationPreferences;
import dev.adriangm.tracker.search.application.port.ProfileRepository;
import org.springframework.stereotype.Component;

@Component
public class ProfileNotificationPreferences implements NotificationPreferences {
    private final ProfileRepository profiles;

    public ProfileNotificationPreferences(ProfileRepository profiles)
    {
        this.profiles = profiles;
    }

    @Override
    public Preferences current()
    {
        var profile = profiles.load();
        var automation = profile.automation();
        return new Preferences(
            profile.externalNotifications(),
            profile.zone(),
            profile.quietFrom(),
            profile.quietUntil(),
            automation.notificationsEnabled(),
            automation.tasks(),
            automation.interviews(),
            automation.newOpportunities()
        );
    }
}
