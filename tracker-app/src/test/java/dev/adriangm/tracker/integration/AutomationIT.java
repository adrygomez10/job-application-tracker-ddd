package dev.adriangm.tracker.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.adriangm.tracker.notifications.api.NotificationMessage;
import dev.adriangm.tracker.notifications.api.NotificationPreferences;
import dev.adriangm.tracker.notifications.api.NotificationRequests;
import dev.adriangm.tracker.notifications.application.collection.CollectNotifications;
import dev.adriangm.tracker.notifications.application.collection.SendReminder;
import dev.adriangm.tracker.notifications.application.port.NotificationChannel;
import dev.adriangm.tracker.notifications.application.port.NotificationQueue;
import dev.adriangm.tracker.notifications.domain.DeliveryState;
import dev.adriangm.tracker.reminders.api.ReminderNotifications;
import dev.adriangm.tracker.search.application.error.FeedUnavailable;
import dev.adriangm.tracker.search.application.feed.ImportFeed;
import dev.adriangm.tracker.search.application.port.FeedRuns;
import dev.adriangm.tracker.search.application.port.OfferFeed;
import dev.adriangm.tracker.search.application.port.OpportunityRepository;
import dev.adriangm.tracker.search.application.port.ProfileRepository;
import dev.adriangm.tracker.search.application.port.SearchWork;
import dev.adriangm.tracker.search.application.query.OpportunityCriteria;
import dev.adriangm.tracker.search.application.query.SearchQueries;
import dev.adriangm.tracker.search.domain.WorkMode;
import dev.adriangm.tracker.search.domain.opportunity.Opportunity;
import dev.adriangm.tracker.search.domain.opportunity.OpportunityDetails;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AutomationIT extends IntegrationTest {
    @Autowired
    NotificationQueue queue;
    @Autowired
    NotificationRequests notifications;
    @Autowired
    OpportunityRepository opportunities;
    @Autowired
    ProfileRepository profiles;
    @Autowired
    FeedRuns runs;
    @Autowired
    SearchWork searchWork;
    private final Instant now = Instant.parse("2026-10-24T10:00:00Z");

    @Test
    void importAndSummaryCommitTogetherAndRepeatedFeedDoesNotCreateAnotherSummary()
    {
        enable();
        var importer = importer(notifications, Clock.fixed(now, ZoneOffset.UTC));
        assertThat(importer.execute().imported()).isEqualTo(1);
        assertThat(importer.execute().imported()).isZero();
        assertThat(queue.pending(25)).hasSize(1);
        assertThat(runs.history(0)).hasSize(2).allMatch(run -> run.state().equals("SUCCEEDED"));
        assertThat(queue.pending(25).getFirst().message().newCount()).isEqualTo(1);
    }

    @Test
    void failedQueueWriteRollsBackImportedDataAndRecordsFailedRun()
    {
        var importer = importer((id, count, instant) -> {
            throw new IllegalStateException("Simulated queue failure");
        }, Clock.fixed(now, ZoneOffset.UTC));
        assertThatThrownBy(importer::execute).hasMessage("Simulated queue failure");
        assertThat(opportunities.recent()).isEmpty();
        assertThat(queue.pending(25)).isEmpty();
        assertThat(runs.latest().orElseThrow().state()).isEqualTo("FAILED");
    }

    @Test
    void scheduledImportHonorsIntervalAndDoesNotReplayMissedRuns()
    {
        enable();
        var clock = mock(Clock.class);
        when(clock.instant()).thenReturn(now);
        var importer = importer(notifications, clock);
        importer.tick();
        importer.tick();
        assertThat(runs.history(0)).hasSize(1);
        when(clock.instant()).thenReturn(now.plus(Duration.ofDays(2)));
        importer.tick();
        importer.tick();
        assertThat(runs.history(0)).hasSize(2);
    }

    @Test
    void pendingWorkRecoversButAnInterruptedAttemptCannotBeClaimedAgain()
    {
        var first = NotificationMessage.summary(UUID.randomUUID(), 1, now);
        var second = NotificationMessage.summary(UUID.randomUUID(), 1, now);
        queue.enqueue(first);
        queue.enqueue(second);
        assertThat(queue.claim(first.key(), now)).isTrue();
        queue.recover(now.plusSeconds(60));
        assertThat(queue.claim(first.key(), now)).isFalse();
        assertThat(queue.pending(25)).extracting(entry -> entry.message().key()).containsExactly(second.key());
        assertThat(queue.history(0)).anyMatch(entry -> entry.state() == DeliveryState.UNCERTAIN);
    }

    @Test
    void twoIndependentRequestsCanOnlyClaimOneDelivery() throws Exception
    {
        var message = NotificationMessage.summary(UUID.randomUUID(), 1, now);
        queue.enqueue(message);
        var start = new CountDownLatch(1);
        Callable<Boolean> contender = () -> {
            start.await();
            return queue.claim(message.key(), now);
        };
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(contender);
            var second = pool.submit(contender);
            start.countDown();
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);
        }
    }

    @Test
    void opportunityFiltersAndPaginationReachOlderRecordsAndTreatWildcardsLiterally()
    {
        for (int index = 0; index < 51; index++) {
            var id = UUID.randomUUID();
            opportunities.create(Opportunity.capture(id, details("Java " + index), "manual:" + id));
        }
        var query = context.getBean(SearchQueries.class);
        var criteria = new OpportunityCriteria("Example", "Java", "REMOTE", "manual", 1);
        assertThat(query.search(criteria)).hasSize(25);
        assertThat(query.hasNext(criteria)).isTrue();
        assertThat(query.search(new OpportunityCriteria("", "", "", "", 2))).hasSize(1);
        assertThat(query.search(new OpportunityCriteria("%", "", "", "", 0))).isEmpty();
    }

    @Test
    void claimingInsideTheImportTransactionIsRejectedBeforeHttpCouldStart()
    {
        var message = NotificationMessage.summary(UUID.randomUUID(), 1, now);
        queue.enqueue(message);
        assertThatThrownBy(() -> searchWork.required(() -> queue.claim(message.key(), now)))
            .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
        assertThat(queue.pending(25)).hasSize(1);
    }

    @Test
    void manualAndAutomaticRequestsRaceForTheSameReminderWithoutDuplicatingIt() throws Exception
    {
        enable();
        var preferences = context.getBean(NotificationPreferences.class);
        UUID activity = UUID.randomUUID();
        ReminderNotifications reminders = () -> List.of(new ReminderNotifications.DueReminder(activity, "TASK", now));
        var channel = mock(NotificationChannel.class);
        when(channel.configured()).thenReturn(true);
        var clock = Clock.fixed(now, ZoneOffset.UTC);
        var send = new SendReminder(preferences, reminders, queue, channel, clock);
        var collect = new CollectNotifications(preferences, reminders, queue, clock);
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var manual = pool.submit(() -> {
                start.await();
                return send.execute(activity);
            });
            var automatic = pool.submit(() -> {
                start.await();
                collect.execute();
                return true;
            });
            start.countDown();
            manual.get();
            automatic.get();
        }
        assertThat(queue.pending(25)).hasSize(1);
        assertThat(queue.pending(25).getFirst().message().activityId()).isEqualTo(activity);
    }

    @Test
    void failedProviderWaitsForItsIntervalBeforeAnotherScheduledAttempt()
    {
        enable();
        jdbc.update("UPDATE search_profile SET import_interval=15");
        var clock = mock(Clock.class);
        when(clock.instant()).thenReturn(now);
        var feed = mock(OfferFeed.class);
        when(feed.configured()).thenReturn(true);
        when(feed.fetch()).thenThrow(new FeedUnavailable("Simulated provider failure")).thenReturn(List.of());
        var importer = new ImportFeed(feed, opportunities, searchWork, profiles, runs, notifications, clock);
        assertThatThrownBy(importer::tick).isInstanceOf(FeedUnavailable.class);
        importer.tick();
        assertThat(runs.history(0)).hasSize(1);
        assertThat(runs.latest().orElseThrow().state()).isEqualTo("FAILED");
        when(clock.instant()).thenReturn(now.plus(Duration.ofMinutes(15)));
        importer.tick();
        assertThat(runs.history(0)).hasSize(2);
        assertThat(runs.latest().orElseThrow().state()).isEqualTo("SUCCEEDED");
    }

    private void enable()
    {
        jdbc.update("""
            UPDATE search_profile SET auto_import=true, auto_notifications=true,
            external_notifications=true
            """);
    }

    private ImportFeed importer(NotificationRequests requests, Clock clock)
    {
        OfferFeed feed = () -> List.of(new OfferFeed.ImportedOffer("test:board:1", details("Java")));
        return new ImportFeed(feed, opportunities, searchWork, profiles, runs, requests, clock);
    }

    private static OpportunityDetails details(String role)
    {
        return new OpportunityDetails("Example", "", role, "Madrid", WorkMode.REMOTE, null, "");
    }
}
