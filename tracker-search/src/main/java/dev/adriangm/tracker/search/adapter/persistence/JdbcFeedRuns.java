package dev.adriangm.tracker.search.adapter.persistence;

import dev.adriangm.tracker.search.application.FeedRuns;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcFeedRuns implements FeedRuns {
    private final JdbcTemplate jdbc;

    public JdbcFeedRuns(JdbcTemplate jdbc)
    {
        this.jdbc = jdbc;
    }

    @Override
    public void begin(UUID id, String origin, Instant now)
    {
        jdbc
            .update(
                "INSERT INTO feed_runs(id, origin, started_at, state) VALUES (?, ?, ?, 'RUNNING')",
                id,
                origin,
                Timestamp.from(now)
            );
    }

    @Override
    public void succeed(UUID id, int imported, int unchanged, Instant now)
    {
        jdbc.update("""
            UPDATE feed_runs SET state='SUCCEEDED', finished_at=?, imported=?, unchanged=? WHERE id=?
            """, Timestamp.from(now), imported, unchanged, id);
    }

    @Override
    public void fail(UUID id, Instant now)
    {
        jdbc.update("UPDATE feed_runs SET state='FAILED', finished_at=? WHERE id=?", Timestamp.from(now), id);
    }

    @Override
    public Optional<Run> latest()
    {
        return jdbc
            .query("SELECT * FROM feed_runs ORDER BY started_at DESC, id LIMIT 1", (row, index) -> map(row))
            .stream()
            .findFirst();
    }

    @Override
    public List<Run> history(int page)
    {
        return jdbc
            .query(
                "SELECT * FROM feed_runs ORDER BY started_at DESC, id LIMIT 25 OFFSET ?",
                (row, index) -> map(row),
                Math.multiplyExact(Math.max(0, page), 25)
            );
    }

    @Override
    public void recover(Instant now)
    {
        jdbc.update("""
            UPDATE feed_runs SET state='INTERRUPTED', finished_at=? WHERE state='RUNNING'
            """, Timestamp.from(now));
    }

    private static Run map(ResultSet row) throws SQLException
    {
        var finished = row.getTimestamp("finished_at");
        return new Run(
            row.getObject("id", UUID.class),
            row.getString("origin"),
            row.getTimestamp("started_at").toInstant(),
            finished == null ? null : finished.toInstant(),
            row.getString("state"),
            row.getInt("imported"),
            row.getInt("unchanged")
        );
    }
}
