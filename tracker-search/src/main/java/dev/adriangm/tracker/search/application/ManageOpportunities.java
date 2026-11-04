package dev.adriangm.tracker.search.application;

import dev.adriangm.tracker.applications.api.ApplicationsApi;
import dev.adriangm.tracker.search.domain.Opportunity;
import dev.adriangm.tracker.search.domain.OpportunityDetails;
import java.util.UUID;

public final class ManageOpportunities {
    private final OpportunityRepository store;
    private final SearchWork work;
    private final ApplicationsApi applications;

    public ManageOpportunities(OpportunityRepository store, SearchWork work, ApplicationsApi applications)
    {
        this.store = store;
        this.work = work;
        this.applications = applications;
    }

    public void capture(UUID id, OpportunityDetails details)
    {
        store.create(Opportunity.capture(id, details, "manual:" + id));
    }

    public void revise(UUID id, long expectedVersion, OpportunityDetails details)
    {
        work.required(() -> {
            Opportunity current = store.find(id).orElseThrow();
            if (current.version() != expectedVersion) {
                throw new SearchConflict();
            }

            store.save(current.revise(details));
            return null;
        });
    }

    public ApplicationsApi.ProcessPage linkableProcesses(String query, int page)
    {
        return applications.linkableProcesses(query, page);
    }

    public void relate(UUID opportunityId, UUID processId, long version)
    {
        work.required(() -> {
            store.find(opportunityId).orElseThrow();
            applications.relateOpportunity(processId, version, opportunityId);
            return null;
        });
    }

    public UUID start(UUID opportunityId, UUID processId, String channel)
    {
        return work.required(() -> {
            Opportunity current = store.find(opportunityId).orElseThrow();
            var details = current.details();
            var offer = new ApplicationsApi.Offer(
                details.employer(),
                details.role(),
                details.url(),
                channel,
                details.location(),
                details.workMode().name(),
                ""
            );

            return applications.startForOpportunity(opportunityId, processId, offer);
        });
    }
}
