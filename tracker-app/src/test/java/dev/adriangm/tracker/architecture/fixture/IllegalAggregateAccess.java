package dev.adriangm.tracker.architecture.fixture;

import dev.adriangm.tracker.applications.domain.activity.FollowUpTask;
import dev.adriangm.tracker.applications.domain.activity.Interview;
import dev.adriangm.tracker.applications.domain.history.TimelineEntry;
import dev.adriangm.tracker.applications.domain.history.TimelineEvent;
import dev.adriangm.tracker.applications.domain.status.Status;
import java.time.Instant;

/** Deliberate violations used to prove that accessible operations remain restricted. */
public class IllegalAggregateAccess {
    public FollowUpTask complete(FollowUpTask task)
    {
        return task.complete();
    }

    public Interview finish(Interview interview)
    {
        return interview.finish(Interview.Outcome.COMPLETED, "Result");
    }

    public TimelineEntry history(Instant now)
    {
        return TimelineEntry.of(now, TimelineEvent.NOTE, "Bypassed aggregate");
    }

    public TimelineEntry statusHistory(Instant now)
    {
        return TimelineEntry.statusChange(now, TimelineEvent.STATUS_CHANGED, Status.SAVED, Status.APPLIED, "");
    }
}
