package dev.adriangm.tracker.applications.application.details;

import dev.adriangm.tracker.applications.application.port.ApplicationStore;
import dev.adriangm.tracker.applications.application.port.UnitOfWork;
import dev.adriangm.tracker.applications.domain.candidacy.ApplicationDetails;
import dev.adriangm.tracker.applications.domain.candidacy.JobApplication;
import dev.adriangm.tracker.applications.domain.status.Status;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Registers a saved offer or a submitted application. The caller supplies the identity so that a
 * repeated submission of the same form is rejected instead of creating a duplicate.
 */
public final class RegisterApplication {
    private final ApplicationStore store;
    private final UnitOfWork work;
    private final Clock clock;

    public RegisterApplication(ApplicationStore store, UnitOfWork work, Clock clock)
    {
        this.store = Objects.requireNonNull(store);
        this.work = Objects.requireNonNull(work);
        this.clock = Objects.requireNonNull(clock);
    }

    public JobApplication execute(UUID id, ApplicationDetails details, Status status, LocalDate appliedOn)
    {
        return work
            .required(
                () -> store
                    .create(
                        JobApplication.create(id, details, status, appliedOn, clock.instant(), LocalDate.now(clock))
                    )
            );
    }
}
