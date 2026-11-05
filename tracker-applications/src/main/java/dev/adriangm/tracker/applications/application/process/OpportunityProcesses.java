package dev.adriangm.tracker.applications.application.process;

import dev.adriangm.tracker.applications.api.ApplicationsApi;
import dev.adriangm.tracker.applications.application.details.RegisterApplication;
import dev.adriangm.tracker.applications.application.port.OpportunityProcessStore;
import dev.adriangm.tracker.applications.application.port.UnitOfWork;
import dev.adriangm.tracker.applications.domain.candidacy.ApplicationDetails;
import dev.adriangm.tracker.applications.domain.policy.ActiveProcessPolicy;
import dev.adriangm.tracker.applications.domain.status.Status;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/** Coordinates local consistency; the output adapter also enforces the rule during races. */
public final class OpportunityProcesses {
    private final OpportunityProcessStore store;
    private final RegisterApplication register;
    private final UnitOfWork work;
    private final Clock clock;

    public OpportunityProcesses(
        OpportunityProcessStore store,
        RegisterApplication register,
        UnitOfWork work,
        Clock clock
    )
    {
        this.store = store;
        this.register = register;
        this.work = work;
        this.clock = clock;
    }

    public UUID start(UUID opportunityId, UUID processId, ApplicationDetails details)
    {
        return work.required(() -> {
            long activeProcesses = store.activeCount(opportunityId);
            new ActiveProcessPolicy().requireAvailable(activeProcesses);

            LocalDate appliedOn = LocalDate.now(clock);
            register.execute(processId, details, Status.APPLIED, appliedOn);
            store.attachNew(processId, opportunityId);

            return processId;
        });
    }

    public void relate(UUID processId, long expectedVersion, UUID opportunityId)
    {
        work.required(() -> {
            store.relate(processId, expectedVersion, opportunityId);
            return null;
        });
    }

    public ApplicationsApi.ProcessPage recent(String query, int page)
    {
        if (page < 0 || page > 1000000 || query.length() > 180) {
            throw new IllegalArgumentException("Invalid process search");
        }

        return store.recent(query, page);
    }
}
