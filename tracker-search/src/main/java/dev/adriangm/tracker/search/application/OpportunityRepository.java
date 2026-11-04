package dev.adriangm.tracker.search.application;

import dev.adriangm.tracker.search.domain.Opportunity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repository for the Opportunity aggregate root, independent of its SQL representation. */
public interface OpportunityRepository {
    List<Opportunity> recent();

    Optional<Opportunity> find(UUID id);

    Optional<Opportunity> bySource(String sourceKey);

    void create(Opportunity opportunity);

    void save(Opportunity opportunity);
}
