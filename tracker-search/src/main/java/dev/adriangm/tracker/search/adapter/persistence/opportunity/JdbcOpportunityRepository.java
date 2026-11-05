package dev.adriangm.tracker.search.adapter.persistence.opportunity;

import dev.adriangm.tracker.search.adapter.persistence.mapping.SalaryColumns;
import dev.adriangm.tracker.search.adapter.persistence.profile.JdbcProfileStore;
import dev.adriangm.tracker.search.application.error.SearchConflict;
import dev.adriangm.tracker.search.application.port.OpportunityRepository;
import dev.adriangm.tracker.search.domain.WorkMode;
import dev.adriangm.tracker.search.domain.opportunity.Opportunity;
import dev.adriangm.tracker.search.domain.opportunity.OpportunityDetails;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOpportunityRepository
    implements
        OpportunityRepository,
        dev.adriangm.tracker.search.application.query.SearchQueries {
    private final JdbcTemplate jdbc;
    private final JdbcProfileStore profiles;

    public JdbcOpportunityRepository(JdbcTemplate jdbc, JdbcProfileStore profiles)
    {
        this.jdbc = jdbc;
        this.profiles = profiles;
    }

    @Override
    public List<Opportunity> recent()
    {
        return jdbc
            .query("SELECT * FROM opportunities ORDER BY created_at DESC, id LIMIT 25", (row, index) -> map(row));
    }

    @Override
    public List<Opportunity> search(dev.adriangm.tracker.search.application.query.OpportunityCriteria criteria)
    {
        return matching(criteria, 25, criteria.page() * 25);
    }

    @Override
    public boolean hasNext(dev.adriangm.tracker.search.application.query.OpportunityCriteria criteria)
    {
        return !matching(criteria, 1, (criteria.page() + 1) * 25).isEmpty();
    }

    private List<Opportunity> matching(
        dev.adriangm.tracker.search.application.query.OpportunityCriteria criteria,
        int limit,
        int offset
    )
    {
        return jdbc
            .query(
                """
                    SELECT * FROM opportunities
                    WHERE employer ILIKE ? ESCAPE '!' AND role ILIKE ? ESCAPE '!'
                      AND (? = '' OR work_mode = ?) AND source_key ILIKE ? ESCAPE '!'
                    ORDER BY created_at DESC, id LIMIT ? OFFSET ?
                    """,
                (row, index) -> map(row),
                pattern(criteria.employer()),
                pattern(criteria.role()),
                criteria.mode(),
                criteria.mode(),
                pattern(criteria.source()),
                limit,
                offset
            );
    }

    private static String pattern(String text)
    {
        return "%" + text.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    @Override
    public Optional<Opportunity> find(UUID id)
    {
        return jdbc
            .query("SELECT * FROM opportunities WHERE id = ?", (row, index) -> map(row), id)
            .stream()
            .findFirst();
    }

    @Override
    public Optional<Opportunity> bySource(String sourceKey)
    {
        return jdbc
            .query("SELECT * FROM opportunities WHERE source_key = ?", (row, index) -> map(row), sourceKey)
            .stream()
            .findFirst();
    }

    @Override
    public void create(Opportunity opportunity)
    {
        var details = opportunity.details();
        var salary = SalaryColumns.values(details.salary());
        try {
            jdbc
                .update(
                    """
                        INSERT INTO opportunities
                        (id, version, employer, agency, role, location, work_mode, url, source_key,
                         salary_min, salary_max, currency, salary_period)
                        VALUES (?, 0, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                    opportunity.id(),
                    details.employer(),
                    details.agency(),
                    details.role(),
                    details.location(),
                    details.workMode().name(),
                    details.url(),
                    opportunity.sourceKey(),
                    salary[0],
                    salary[1],
                    salary[2],
                    salary[3]
                );
        } catch (DuplicateKeyException duplicate) {
            throw new SearchConflict(duplicate);
        }
    }

    @Override
    public void save(Opportunity opportunity)
    {
        var details = opportunity.details();
        var salary = SalaryColumns.values(details.salary());
        int changed = jdbc
            .update(
                """
                    UPDATE opportunities SET version = version + 1, employer = ?, agency = ?, role = ?,
                      location = ?, work_mode = ?, url = ?, salary_min = ?, salary_max = ?, currency = ?,
                      salary_period = ? WHERE id = ? AND version = ?
                    """,
                details.employer(),
                details.agency(),
                details.role(),
                details.location(),
                details.workMode().name(),
                details.url(),
                salary[0],
                salary[1],
                salary[2],
                salary[3],
                opportunity.id(),
                opportunity.version()
            );
        if (changed != 1) {
            throw new SearchConflict();
        }
    }

    @Override
    public dev.adriangm.tracker.search.domain.profile.SearchProfile profile()
    {
        return profiles.load();
    }

    private static Opportunity map(ResultSet row) throws SQLException
    {
        var details = new OpportunityDetails(
            row.getString("employer"),
            row.getString("agency"),
            row.getString("role"),
            row.getString("location"),
            WorkMode.valueOf(row.getString("work_mode")),
            SalaryColumns.read(row),
            row.getString("url")
        );
        return Opportunity
            .reconstitute(
                row.getObject("id", UUID.class),
                row.getLong("version"),
                details,
                row.getString("source_key")
            );
    }
}
