package dev.adriangm.tracker.search.domain.opportunity;

import dev.adriangm.tracker.search.domain.validation.Text;
import java.util.Objects;
import java.util.UUID;

/** Aggregate root. Editing details never changes identity or external provenance. */
public final class Opportunity {
    private final UUID id;
    private final long version;
    private final OpportunityDetails details;
    private final String sourceKey;

    private Opportunity(UUID id, long version, OpportunityDetails details, String sourceKey)
    {
        this.id = Objects.requireNonNull(id);
        this.version = version;
        this.details = Objects.requireNonNull(details);
        this.sourceKey = Text.required(sourceKey, 240);
        if (version < 0) {
            throw new IllegalArgumentException("Invalid opportunity version");
        }
    }

    public static Opportunity capture(UUID id, OpportunityDetails details, String sourceKey)
    {
        return new Opportunity(id, 0, details, sourceKey);
    }

    /** Persistence reconstruction; architecture checks restrict callers. */
    public static Opportunity reconstitute(UUID id, long version, OpportunityDetails details, String sourceKey)
    {
        return new Opportunity(id, version, details, sourceKey);
    }

    public Opportunity revise(OpportunityDetails next)
    {
        return new Opportunity(id, version, next, sourceKey);
    }

    public UUID id()
    {
        return id;
    }

    public long version()
    {
        return version;
    }

    public OpportunityDetails details()
    {
        return details;
    }

    public String sourceKey()
    {
        return sourceKey;
    }
}
