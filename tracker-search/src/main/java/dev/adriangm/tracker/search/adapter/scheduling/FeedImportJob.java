package dev.adriangm.tracker.search.adapter.scheduling;

import dev.adriangm.tracker.search.application.FeedRuns;
import dev.adriangm.tracker.search.application.ImportFeed;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FeedImportJob {
    private static final Logger LOG = LoggerFactory.getLogger(FeedImportJob.class);
    private final ImportFeed imports;
    private final FeedRuns runs;
    private final Clock clock;

    public FeedImportJob(ImportFeed imports, FeedRuns runs, Clock clock)
    {
        this.imports = imports;
        this.runs = runs;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recover()
    {
        runs.recover(clock.instant());
    }

    @Scheduled(
        fixedDelayString = "${tracker.automation.interval-ms:60000}",
        initialDelayString = "${tracker.automation.interval-ms:60000}"
    )
    public void run()
    {
        try {
            imports.tick();
        } catch (org.springframework.dao.DataAccessException | dev.adriangm.tracker.search.application.FeedUnavailable
            | dev.adriangm.tracker.search.application.SearchConflict failure) {
            LOG.warn("Feed import failed: {}", failure.getClass().getSimpleName());
        }
    }
}
