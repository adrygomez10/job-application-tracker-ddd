package dev.adriangm.tracker.search.application;

import dev.adriangm.tracker.search.domain.Opportunity;
import dev.adriangm.tracker.search.domain.SearchProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SearchQueries {
    List<Opportunity> recent();

    List<Opportunity> search(OpportunityCriteria criteria);

    boolean hasNext(OpportunityCriteria criteria);

    Optional<Opportunity> find(UUID id);

    SearchProfile profile();
}
