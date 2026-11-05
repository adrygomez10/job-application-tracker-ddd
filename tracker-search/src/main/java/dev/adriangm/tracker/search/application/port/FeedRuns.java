package dev.adriangm.tracker.search.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeedRuns {
    void begin(UUID id, String origin, Instant now);

    void succeed(UUID id, int imported, int unchanged, Instant now);

    void fail(UUID id, Instant now);

    Optional<Run> latest();

    List<Run> history(int page);

    void recover(Instant now);

    record Run(
        UUID id,
        String origin,
        Instant startedAt,
        Instant finishedAt,
        String state,
        int imported,
        int unchanged
    ) {
    }
}
