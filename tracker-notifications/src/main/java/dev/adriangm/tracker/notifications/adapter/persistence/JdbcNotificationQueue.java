package dev.adriangm.tracker.notifications.adapter.persistence;

import dev.adriangm.tracker.notifications.api.NotificationMessage;
import dev.adriangm.tracker.notifications.application.port.NotificationQueue;
import dev.adriangm.tracker.notifications.domain.DeliveryState;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcNotificationQueue implements NotificationQueue {
    private final JdbcTemplate jdbc;

    public JdbcNotificationQueue(JdbcTemplate jdbc)
    {
        this.jdbc = jdbc;
    }

    @Override
    public boolean enqueue(NotificationMessage message)
    {
        return jdbc
            .update(
                """
                    INSERT INTO notification_queue
                    (message_key, kind, activity_id, due_at, new_count, automatic, created_at, expires_at,
                     state, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', ?)
                    ON CONFLICT DO NOTHING
                    """,
                message.key(),
                message.kind(),
                message.activityId(),
                timestamp(message.dueAt()),
                message.newCount(),
                message.automatic(),
                timestamp(message.createdAt()),
                timestamp(message.expiresAt()),
                timestamp(message.createdAt())
            ) == 1;
    }

    @Override
    public List<Entry> pending(int limit)
    {
        return jdbc.query("""
            SELECT * FROM notification_queue WHERE state = 'PENDING'
            ORDER BY created_at, message_key LIMIT ?
            """, (row, index) -> map(row), limit);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public boolean claim(String key, Instant now)
    {
        return jdbc.update("""
            UPDATE notification_queue SET state='SENDING', updated_at=?
            WHERE message_key=? AND state='PENDING'
            """, timestamp(now), key) == 1;
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void finish(String key, DeliveryState state, Instant now)
    {
        jdbc.update("""
            UPDATE notification_queue SET state=?, updated_at=?
            WHERE message_key=? AND state='SENDING'
            """, state.name(), timestamp(now), key);
    }

    @Override
    public void dismiss(String key, DeliveryState state, Instant now)
    {
        jdbc.update("""
            UPDATE notification_queue SET state=?, updated_at=?
            WHERE message_key=? AND state='PENDING'
            """, state.name(), timestamp(now), key);
    }

    @Override
    public void recover(Instant now)
    {
        jdbc.update("""
            UPDATE notification_queue SET state='UNCERTAIN', updated_at=? WHERE state='SENDING'
            """, timestamp(now));
    }

    @Override
    public List<Entry> history(int page)
    {
        return jdbc.query("""
            SELECT * FROM notification_queue ORDER BY updated_at DESC, message_key LIMIT 25 OFFSET ?
            """, (row, index) -> map(row), Math.multiplyExact(Math.max(0, page), 25));
    }

    private static Timestamp timestamp(Instant value)
    {
        return value == null ? null : Timestamp.from(value);
    }

    private static Instant instant(ResultSet row, String column) throws SQLException
    {
        var value = row.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static Entry map(ResultSet row) throws SQLException
    {
        var message = new NotificationMessage(
            row.getString("message_key"),
            row.getString("kind"),
            row.getObject("activity_id", UUID.class),
            instant(row, "due_at"),
            row.getInt("new_count"),
            row.getBoolean("automatic"),
            instant(row, "created_at"),
            instant(row, "expires_at")
        );
        return new Entry(message, DeliveryState.valueOf(row.getString("state")), instant(row, "updated_at"));
    }
}
