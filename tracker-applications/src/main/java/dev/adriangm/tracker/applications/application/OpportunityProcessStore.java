package dev.adriangm.tracker.applications.application;

import dev.adriangm.tracker.applications.api.ApplicationsApi;
import java.util.UUID;

/** Output port for process identity relationships, owned by the process module. */
public interface OpportunityProcessStore {
    long activeCount(UUID opportunityId);

    void attachNew(UUID processId, UUID opportunityId);

    void relate(UUID processId, long expectedVersion, UUID opportunityId);

    ApplicationsApi.ProcessPage recent(String query, int page);
}
