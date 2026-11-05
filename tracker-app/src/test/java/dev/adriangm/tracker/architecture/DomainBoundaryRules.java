package dev.adriangm.tracker.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchRule;
import dev.adriangm.tracker.applications.domain.activity.FollowUpTask;
import dev.adriangm.tracker.applications.domain.activity.Interview;
import dev.adriangm.tracker.applications.domain.candidacy.JobApplication;
import dev.adriangm.tracker.applications.domain.history.TimelineEntry;
import dev.adriangm.tracker.applications.domain.history.TimelineEvent;
import dev.adriangm.tracker.applications.domain.status.Status;
import java.time.Instant;

final class DomainBoundaryRules {
    private static final DescribedPredicate<JavaClass> OUTSIDE_ROOT = DescribedPredicate
        .describe(
            "are not the candidacy aggregate root",
            type -> !type.getName().equals(JobApplication.class.getName())
        );

    static final ArchRule TASK_EDITS = restrict(FollowUpTask.class, "complete");
    static final ArchRule INTERVIEW_EDITS = restrict(Interview.class, "finish", Interview.Outcome.class, String.class);
    static final ArchRule HISTORY_ENTRIES = restrict(
        TimelineEntry.class,
        "of",
        Instant.class,
        TimelineEvent.class,
        String.class
    );
    static final ArchRule STATUS_HISTORY = restrict(
        TimelineEntry.class,
        "statusChange",
        Instant.class,
        TimelineEvent.class,
        Status.class,
        Status.class,
        String.class
    );

    private static ArchRule restrict(Class<?> owner, String method, Class<?>... parameters)
    {
        return noClasses()
            .that(OUTSIDE_ROOT)
            .should()
            .callMethod(owner, method, parameters)
            .because("activity edits and history entries must pass through the candidacy aggregate root");
    }

    private DomainBoundaryRules()
    {
    }
}
