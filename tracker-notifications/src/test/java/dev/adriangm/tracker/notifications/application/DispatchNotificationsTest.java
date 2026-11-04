package dev.adriangm.tracker.notifications.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.adriangm.tracker.notifications.api.NotificationMessage;
import dev.adriangm.tracker.notifications.api.NotificationPreferences;
import dev.adriangm.tracker.notifications.domain.DeliveryState;
import dev.adriangm.tracker.reminders.api.ReminderNotifications;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DispatchNotificationsTest {
    private final NotificationQueue queue = mock(NotificationQueue.class);
    private final NotificationChannel channel = mock(NotificationChannel.class);
    private final NotificationPreferences preferences = mock(NotificationPreferences.class);
    private final ReminderNotifications reminders = mock(ReminderNotifications.class);
    private final Clock clock = mock(Clock.class);
    private final Instant now = Instant.parse("2026-10-24T10:00:00Z");
    private final NotificationMessage message = NotificationMessage.summary(UUID.randomUUID(), 3, now);
    private final DispatchNotifications dispatch = new DispatchNotifications(
        queue,
        preferences,
        reminders,
        channel,
        clock
    );

    @BeforeEach
    void prepare()
    {
        when(clock.instant()).thenReturn(now);
        when(queue.pending(25)).thenReturn(List.of(new NotificationQueue.Entry(message, DeliveryState.PENDING, now)));
        when(channel.configured()).thenReturn(true);
        settings(true, true, true);
    }

    @Test
    void sendingStartsOnlyAfterDurableClaimAndNeverRetriesUncertainty()
    {
        when(queue.claim(message.key(), now)).thenReturn(true, false);
        when(channel.send(message)).thenReturn(DeliveryState.UNCERTAIN);
        dispatch.execute();
        dispatch.execute();
        var order = inOrder(queue, channel);
        order.verify(queue).claim(message.key(), now);
        order.verify(channel).send(message);
        order.verify(queue).finish(message.key(), DeliveryState.UNCERTAIN, now);
        verify(channel, times(1)).send(message);
    }

    @Test
    void disabledCategoryCancelsInsteadOfWaitingForReactivation()
    {
        settings(true, true, false);
        dispatch.execute();
        verify(queue).dismiss(message.key(), DeliveryState.CANCELLED, now);
        verify(channel, never()).send(any());
    }

    @Test
    void expiredSummaryIsNotSentAfterAQuietDay()
    {
        when(clock.instant()).thenReturn(now.plusSeconds(86400));
        dispatch.execute();
        verify(queue).dismiss(message.key(), DeliveryState.EXPIRED, now.plusSeconds(86400));
        verify(channel, never()).send(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-10-25T00:30:00Z", "2026-10-25T01:30:00Z"})
    void bothOccurrencesOfRepeatedDstHourRespectSilence(String instant)
    {
        when(clock.instant()).thenReturn(Instant.parse(instant));
        dispatch.execute();
        verify(queue, never()).claim(any(), any());
    }

    @Test
    void completedOrRescheduledActivityIsCancelledUsingFreshSource()
    {
        var reminder = NotificationMessage.reminder(UUID.randomUUID(), "TASK", now, true, now);
        when(queue.pending(25)).thenReturn(List.of(new NotificationQueue.Entry(reminder, DeliveryState.PENDING, now)));
        when(reminders.active()).thenReturn(List.of());
        dispatch.execute();
        verify(queue).dismiss(reminder.key(), DeliveryState.CANCELLED, now);
        verify(channel, never()).send(any());
    }

    @Test
    void revokedConsentCancelsExplicitRequestsToo()
    {
        settings(false, true, true);
        dispatch.execute();
        verify(queue).dismiss(message.key(), DeliveryState.CANCELLED, now);
    }

    @Test
    void summaryContentContainsOnlyTheCount()
    {
        assertThat(message.text()).isEqualTo("Job Tracker found 3 new opportunities.");
    }

    private void settings(boolean consent, boolean automatic, boolean opportunities)
    {
        when(preferences.current())
            .thenReturn(
                new NotificationPreferences.Preferences(
                    consent,
                    ZoneId.of("Europe/Madrid"),
                    LocalTime.of(22, 0),
                    LocalTime.of(8, 0),
                    automatic,
                    true,
                    true,
                    opportunities
                )
            );
    }
}
