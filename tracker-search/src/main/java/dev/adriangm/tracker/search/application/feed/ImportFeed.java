package dev.adriangm.tracker.search.application.feed;

import dev.adriangm.tracker.notifications.api.NotificationRequests;
import dev.adriangm.tracker.search.application.port.FeedRuns;
import dev.adriangm.tracker.search.application.port.OfferFeed;
import dev.adriangm.tracker.search.application.port.OpportunityRepository;
import dev.adriangm.tracker.search.application.port.ProfileRepository;
import dev.adriangm.tracker.search.application.port.SearchWork;
import dev.adriangm.tracker.search.domain.opportunity.Opportunity;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

/** One local instance. Manual and scheduled calls serialize through the same use case. */
public final class ImportFeed {
    private final OfferFeed feed;
    private final OpportunityRepository store;
    private final SearchWork work;
    private final ProfileRepository profiles;
    private final FeedRuns runs;
    private final NotificationRequests notifications;
    private final Clock clock;

    public ImportFeed(
        OfferFeed feed,
        OpportunityRepository store,
        SearchWork work,
        ProfileRepository profiles,
        FeedRuns runs,
        NotificationRequests notifications,
        Clock clock
    )
    {
        this.feed = feed;
        this.store = store;
        this.work = work;
        this.profiles = profiles;
        this.runs = runs;
        this.notifications = notifications;
        this.clock = clock;
    }

    public synchronized Result execute()
    {
        return importOffers("MANUAL");
    }

    public synchronized void tick()
    {
        var settings = profiles.load().automation();
        if (!settings.importEnabled() || !feed.configured()) {
            return;
        }

        var previous = runs.latest();
        if (previous.isPresent()) {
            var run = previous.get();
            var completed = run.finishedAt() == null ? run.startedAt() : run.finishedAt();
            if (clock.instant().isBefore(completed.plus(Duration.ofMinutes(settings.importIntervalMinutes())))) {
                return;
            }
        }

        importOffers("AUTOMATIC");
    }

    private Result importOffers(String origin)
    {
        UUID runId = UUID.randomUUID();
        runs.begin(runId, origin, clock.instant());
        boolean completed = false;

        try {
            var offers = feed.fetch();

            var result = work.required(() -> {
                int imported = 0;
                for (var offer : offers) {
                    if (store.bySource(offer.sourceKey()).isEmpty()) {
                        store.create(Opportunity.capture(UUID.randomUUID(), offer.details(), offer.sourceKey()));
                        imported++;
                    }
                }
                int unchanged = offers.size() - imported;

                runs.succeed(runId, imported, unchanged, clock.instant());
                notifications.newOpportunities(runId, imported, clock.instant());

                return new Result(imported, unchanged);
            });
            completed = true;

            return result;
        } finally {
            if (!completed) {
                runs.fail(runId, clock.instant());
            }
        }
    }

    public record Result(int imported, int unchanged) {
    }
}
