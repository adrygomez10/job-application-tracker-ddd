package dev.adriangm.tracker.search.application;

import dev.adriangm.tracker.search.domain.OpportunityDetails;
import java.util.List;

/** A configured provider translates its vocabulary before crossing this port. */
public interface OfferFeed {
    List<ImportedOffer> fetch();

    default boolean configured()
    {
        return true;
    }

    record ImportedOffer(String sourceKey, OpportunityDetails details) {
    }
}
