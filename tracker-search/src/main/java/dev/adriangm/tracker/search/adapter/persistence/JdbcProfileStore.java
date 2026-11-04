package dev.adriangm.tracker.search.adapter.persistence;

import dev.adriangm.tracker.search.application.SearchConflict;
import dev.adriangm.tracker.search.domain.SearchProfile;
import dev.adriangm.tracker.search.domain.WorkMode;
import java.time.LocalTime;
import java.time.ZoneId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcProfileStore implements dev.adriangm.tracker.search.application.ProfileRepository {
    private final JdbcTemplate jdbc;

    public JdbcProfileStore(JdbcTemplate jdbc)
    {
        this.jdbc = jdbc;
    }

    public SearchProfile load()
    {
        return jdbc
            .queryForObject(
                "SELECT * FROM search_profile WHERE id = 1",
                (row, index) -> new SearchProfile(
                    row.getLong("version"),
                    row.getString("display_name"),
                    row.getString("target_role"),
                    row.getString("location"),
                    WorkMode.valueOf(row.getString("work_mode")),
                    SalaryColumns.read(row),
                    ZoneId.of(row.getString("zone")),
                    row.getBoolean("external_notifications"),
                    row.getObject("quiet_from", LocalTime.class),
                    row.getObject("quiet_until", LocalTime.class),
                    new dev.adriangm.tracker.search.domain.AutomationSettings(
                        row.getBoolean("auto_import"),
                        row.getInt("import_interval"),
                        row.getBoolean("auto_notifications"),
                        row.getBoolean("notify_tasks"),
                        row.getBoolean("notify_interviews"),
                        row.getBoolean("notify_opportunities")
                    )
                )
            );
    }

    public void save(SearchProfile profile)
    {
        var salary = SalaryColumns.values(profile.targetSalary());
        int changed = jdbc
            .update(
                """
                    UPDATE search_profile SET version = version + 1, display_name = ?, target_role = ?,
                      location = ?, work_mode = ?, salary_min = ?, salary_max = ?, currency = ?,
                      salary_period = ?, zone = ?, external_notifications = ?, quiet_from = ?, quiet_until = ?,
                      auto_import = ?, import_interval = ?, auto_notifications = ?, notify_tasks = ?,
                      notify_interviews = ?, notify_opportunities = ?
                    WHERE id = 1 AND version = ?
                    """,
                profile.displayName(),
                profile.targetRole(),
                profile.location(),
                profile.preferredMode().name(),
                salary[0],
                salary[1],
                salary[2],
                salary[3],
                profile.zone().getId(),
                profile.externalNotifications(),
                profile.quietFrom(),
                profile.quietUntil(),
                profile.automation().importEnabled(),
                profile.automation().importIntervalMinutes(),
                profile.automation().notificationsEnabled(),
                profile.automation().tasks(),
                profile.automation().interviews(),
                profile.automation().newOpportunities(),
                profile.version()
            );
        if (changed != 1) {
            throw new SearchConflict();
        }
    }
}
