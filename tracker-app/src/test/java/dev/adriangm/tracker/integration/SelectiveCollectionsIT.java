package dev.adriangm.tracker.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class SelectiveCollectionsIT extends IntegrationTest {
    @Test
    void appendingToLongHistoryDoesNotRewriteExistingRows()
    {
        var saved = seed();
        work.required(() -> {
            var current = store.find(saved.id()).orElseThrow();
            var changed = current;
            for (int index = 0; index < 500; index++) {
                changed = changed.addNote("Synthetic note " + index, Instant.now());
            }
            store.save(changed, current.version());
            return null;
        });
        String before = jdbc.queryForObject("""
            SELECT xmin::text FROM application_timeline WHERE application_id = ? AND position = 0
            """, String.class, saved.id());
        work.required(() -> {
            var current = store.find(saved.id()).orElseThrow();
            store.save(current.addNote("One more note", Instant.now()), current.version());
            return null;
        });
        String after = jdbc.queryForObject("""
            SELECT xmin::text FROM application_timeline WHERE application_id = ? AND position = 0
            """, String.class, saved.id());
        assertThat(after).isEqualTo(before);
        assertThat(
            jdbc
                .queryForObject(
                    "SELECT count(*) FROM application_timeline WHERE application_id = ?",
                    Integer.class,
                    saved.id()
                )
        ).isEqualTo(502);
    }
}
