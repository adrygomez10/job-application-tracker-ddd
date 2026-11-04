package dev.adriangm.tracker.notifications.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.adriangm.tracker.notifications.api.NotificationPreferences;
import dev.adriangm.tracker.reminders.api.ReminderNotifications;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SendReminderTest {
    private final NotificationQueue queue = mock(NotificationQueue.class);
    private final NotificationChannel channel = mock(NotificationChannel.class);
    private final NotificationPreferences preferences = mock(NotificationPreferences.class);
    private final ReminderNotifications reminders = mock(ReminderNotifications.class);
    private final UUID id = UUID.randomUUID();
    private final Clock clock = Clock.fixed(Instant.EPOCH, ZoneId.of("UTC"));
    private final SendReminder send = new SendReminder(preferences, reminders, queue, channel, clock);

    @Test
    void disabledConsentNeverEnqueuesOrContactsProvider()
    {
        settings(false);
        assertThat(send.execute(id)).isEqualTo(SendReminder.Result.DISABLED);
        verifyNoInteractions(queue, channel, reminders);
    }

    @Test
    void manualRequestCanWaitThroughSilenceWithoutDuplicateOrSynchronousHttp()
    {
        settings(true);
        when(channel.configured()).thenReturn(true);
        when(reminders.active()).thenReturn(List.of(new ReminderNotifications.DueReminder(id, "TASK", Instant.EPOCH)));
        when(queue.enqueue(any())).thenReturn(true, false);
        assertThat(send.execute(id)).isEqualTo(SendReminder.Result.QUEUED);
        assertThat(send.execute(id)).isEqualTo(SendReminder.Result.ALREADY_ATTEMPTED);
        verify(channel, never()).send(any());
    }

    private void settings(boolean enabled)
    {
        when(preferences.current())
            .thenReturn(
                new NotificationPreferences.Preferences(
                    enabled,
                    ZoneId.of("Europe/Madrid"),
                    LocalTime.of(22, 0),
                    LocalTime.of(8, 0),
                    false,
                    true,
                    true,
                    true
                )
            );
    }
}
