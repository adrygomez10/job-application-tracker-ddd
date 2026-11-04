package dev.adriangm.tracker.notifications.adapter.scheduling;

import dev.adriangm.tracker.notifications.application.CollectNotifications;
import dev.adriangm.tracker.notifications.application.DispatchNotifications;
import dev.adriangm.tracker.notifications.application.NotificationQueue;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationJob {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationJob.class);
    private final CollectNotifications collect;
    private final DispatchNotifications dispatch;
    private final NotificationQueue queue;
    private final Clock clock;

    public NotificationJob(
        CollectNotifications collect,
        DispatchNotifications dispatch,
        NotificationQueue queue,
        Clock clock
    )
    {
        this.collect = collect;
        this.dispatch = dispatch;
        this.queue = queue;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recover()
    {
        queue.recover(clock.instant());
    }

    @Scheduled(
        fixedDelayString = "${tracker.automation.interval-ms:60000}",
        initialDelayString = "${tracker.automation.interval-ms:60000}"
    )
    public void run()
    {
        try {
            collect.execute();
            dispatch.execute();
        } catch (org.springframework.dao.DataAccessException failure) {
            LOG.warn("Notification cycle failed: {}", failure.getClass().getSimpleName());
        }
    }
}
