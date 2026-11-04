package dev.adriangm.tracker.applications.adapter.persistence;

import dev.adriangm.tracker.applications.api.ApplicationsApi;
import dev.adriangm.tracker.applications.application.EditConflict;
import dev.adriangm.tracker.applications.application.OpportunityProcessStore;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOpportunityProcesses implements OpportunityProcessStore {
    private final JdbcTemplate jdbc;

    public JdbcOpportunityProcesses(JdbcTemplate jdbc)
    {
        this.jdbc = jdbc;
    }

    @Override
    public long activeCount(UUID opportunityId)
    {
        return jdbc.queryForObject("""
            SELECT count(*) FROM job_applications WHERE opportunity_id = ?
              AND status IN ('APPLIED','IN_PROGRESS','OFFER_RECEIVED')
            """, Long.class, opportunityId);
    }

    @Override
    public ApplicationsApi.ProcessPage recent(String query, int page)
    {
        var items = jdbc
            .query(
                """
                    SELECT id, version, company, role, status FROM job_applications
                    WHERE company ILIKE ? ESCAPE '!' OR role ILIKE ? ESCAPE '!'
                    ORDER BY updated_at DESC, id LIMIT 26 OFFSET ?
                    """,
                (row, index) -> new ApplicationsApi.ProcessLinkView(
                    row.getObject("id", UUID.class),
                    row.getLong("version"),
                    row.getString("company"),
                    row.getString("role"),
                    row.getString("status")
                ),
                pattern(query),
                pattern(query),
                page * 25
            );
        return new ApplicationsApi.ProcessPage(items.stream().limit(25).toList(), items.size() > 25);
    }

    private static String pattern(String query)
    {
        return "%" + query.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    @Override
    public void attachNew(UUID processId, UUID opportunityId)
    {
        try {
            jdbc.update("UPDATE job_applications SET opportunity_id = ? WHERE id = ?", opportunityId, processId);
        } catch (DuplicateKeyException conflict) {
            throw new EditConflict(conflict);
        }
    }

    @Override
    public void relate(UUID processId, long expectedVersion, UUID opportunityId)
    {
        UUID previous = jdbc
            .queryForObject(
                "SELECT opportunity_id FROM job_applications WHERE id = ? FOR UPDATE",
                UUID.class,
                processId
            );
        try {
            int changed = jdbc.update("""
                UPDATE job_applications SET opportunity_id = ?, version = version + 1
                WHERE id = ? AND version = ?
                """, opportunityId, processId, expectedVersion);
            if (changed != 1) {
                throw new EditConflict();
            }
            jdbc.update("""
                INSERT INTO process_opportunity_history
                  (process_id, previous_opportunity, next_opportunity)
                VALUES (?, ?, ?)
                """, processId, previous, opportunityId);
        } catch (DuplicateKeyException conflict) {
            throw new EditConflict(conflict);
        }
    }
}
