package dev.adriangm.tracker.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DddMigrationIT extends IntegrationTest {
    @Autowired
    DataSource dataSource;

    @Test
    void upgradesWorkshopTwoWithoutGuessingIdentityOrAgency()
    {
        String schema = "workshop_three_migration";
        upgrade(schema, "3");
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        try {
            seedLegacy(first);
            seedLegacy(second);
            upgrade(schema, "latest");
            assertThat(
                jdbc.queryForObject("SELECT count(*) FROM workshop_three_migration.opportunities", Integer.class)
            ).isEqualTo(2);
            assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM workshop_three_migration.job_applications
                WHERE opportunity_id = id AND version = 7 AND notes = 'Preserved note'
                """, Integer.class)).isEqualTo(2);
            assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM workshop_three_migration.opportunities
                WHERE employer = 'Same text' AND agency = '' AND salary_min IS NULL
                """, Integer.class)).isEqualTo(2);
        } finally {
            jdbc.execute("DROP SCHEMA workshop_three_migration CASCADE");
        }
    }

    @Test
    void upgradesVersionSixWithoutAllowingOldDeliveriesToBeSentAgain()
    {
        String schema = "version_six_upgrade";
        upgrade(schema, "6");
        var due = java.time.Instant.parse("2026-10-24T10:00:00.123456Z");
        UUID activity = UUID.randomUUID();
        try {
            jdbc.update("""
                INSERT INTO version_six_upgrade.notification_deliveries
                VALUES (?, 'TASK', ?, 'SENDING', ?)
                """, activity, java.sql.Timestamp.from(due), java.sql.Timestamp.from(due));
            upgrade(schema, "latest");
            var expected = dev.adriangm.tracker.notifications.api.NotificationMessage
                .reminder(activity, "TASK", due, false, due);
            assertThat(
                jdbc.queryForObject("SELECT message_key FROM version_six_upgrade.notification_queue", String.class)
            ).isEqualTo(expected.key());
            assertThat(jdbc.queryForObject("SELECT state FROM version_six_upgrade.notification_queue", String.class))
                .isEqualTo("UNCERTAIN");
            assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM version_six_upgrade.search_profile
                WHERE auto_import=false AND auto_notifications=false AND import_interval=60
                """, Integer.class)).isEqualTo(1);
        } finally {
            jdbc.execute("DROP SCHEMA version_six_upgrade CASCADE");
        }
    }

    private void upgrade(String schema, String target)
    {
        Flyway
            .configure()
            .dataSource(dataSource)
            .schemas(schema)
            .defaultSchema(schema)
            .locations("classpath:db/migration")
            .target(target)
            .load()
            .migrate();
    }

    private void seedLegacy(UUID id)
    {
        jdbc.update("""
            INSERT INTO workshop_three_migration.job_applications VALUES
            (?,7,3,'Same text','Java','','','','UNSPECIFIED','','','Preserved note',
            'APPLIED','2026-10-01',false,'2026-10-01T10:00:00Z','2026-10-02T10:00:00Z')
            """, id);
    }
}
