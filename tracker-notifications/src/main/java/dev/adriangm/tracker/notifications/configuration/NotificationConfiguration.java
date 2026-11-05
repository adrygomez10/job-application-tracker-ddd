package dev.adriangm.tracker.notifications.configuration;

import dev.adriangm.tracker.notifications.adapter.http.NtfyChannel;
import dev.adriangm.tracker.notifications.api.NotificationPreferences;
import dev.adriangm.tracker.notifications.application.collection.SendReminder;
import dev.adriangm.tracker.notifications.application.port.NotificationChannel;
import dev.adriangm.tracker.notifications.domain.DeliveryState;
import dev.adriangm.tracker.reminders.api.ReminderNotifications;
import java.net.URI;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationConfiguration {
    @Bean
    NotificationChannel notificationChannel(
        @Value("${tracker.ntfy.topic:}") String topic,
        @Value("${tracker.ntfy.token:}") String token
    )
    {
        if (topic.isBlank()) {
            return new NotificationChannel() {
                @Override
                public boolean configured()
                {
                    return false;
                }

                @Override
                public DeliveryState send(dev.adriangm.tracker.notifications.api.NotificationMessage message)
                {
                    throw new IllegalStateException("Channel is disabled");
                }
            };
        }
        URI address = URI.create(topic);
        boolean embeddedOptions = address.getUserInfo() != null || address.getQuery() != null
            || address.getFragment() != null;
        if (!"https".equals(address.getScheme()) || address.getHost() == null || embeddedOptions) {
            throw new IllegalArgumentException("Expected an HTTPS ntfy topic without embedded credentials");
        }
        return new NtfyChannel(address, token);
    }

    @Bean
    dev.adriangm.tracker.notifications.application.collection.CollectNotifications collectNotifications(
        NotificationPreferences preferences,
        ReminderNotifications reminders,
        dev.adriangm.tracker.notifications.application.port.NotificationQueue queue,
        Clock clock
    )
    {
        return new dev.adriangm.tracker.notifications.application.collection.CollectNotifications(
            preferences,
            reminders,
            queue,
            clock
        );
    }

    @Bean
    dev.adriangm.tracker.notifications.application.delivery.DispatchNotifications dispatchNotifications(
        NotificationPreferences preferences,
        ReminderNotifications reminders,
        dev.adriangm.tracker.notifications.application.port.NotificationQueue queue,
        NotificationChannel channel,
        Clock clock
    )
    {
        return new dev.adriangm.tracker.notifications.application.delivery.DispatchNotifications(
            queue,
            preferences,
            reminders,
            channel,
            clock
        );
    }

    @Bean
    SendReminder sendReminder(
        NotificationPreferences preferences,
        ReminderNotifications reminders,
        dev.adriangm.tracker.notifications.application.port.NotificationQueue store,
        NotificationChannel channel,
        Clock clock
    )
    {
        return new SendReminder(preferences, reminders, store, channel, clock);
    }
}
