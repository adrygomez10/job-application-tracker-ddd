package dev.adriangm.tracker.applications.adapter.persistence.candidacy;

import dev.adriangm.tracker.applications.application.error.EditConflict;
import dev.adriangm.tracker.applications.application.error.RecordMissing;
import dev.adriangm.tracker.applications.application.port.ApplicationStore;
import dev.adriangm.tracker.applications.domain.candidacy.JobApplication;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

/** Implements the write contract with JPA. Callers run it inside {@code UnitOfWork}. */
@Repository
public class JpaApplicationStore implements ApplicationStore {
    private static final String UNIQUE_VIOLATION = "23505";

    private final ApplicationJpaRepository repository;

    public JpaApplicationStore(ApplicationJpaRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public Optional<JobApplication> find(UUID id)
    {
        return repository.findById(id).map(ApplicationRow::domain);
    }

    @Override
    public JobApplication create(JobApplication application)
    {
        if (repository.existsById(application.id())) {
            throw new EditConflict();
        }
        try {
            return repository.saveAndFlush(ApplicationRow.from(application)).domain();
        } catch (DataIntegrityViolationException rejected) {
            if (isDuplicateIdentity(rejected)) {
                throw new EditConflict(rejected);
            }
            throw rejected;
        }
    }

    @Override
    public JobApplication save(JobApplication application, long expectedVersion)
    {
        var row = repository.findById(application.id()).orElseThrow(RecordMissing::new);
        if (row.version != expectedVersion) {
            throw new EditConflict();
        }
        row.assign(application);
        try {
            repository.flush();
            return row.domain();
        } catch (DataIntegrityViolationException rejected) {
            if (isDuplicateIdentity(rejected)) {
                throw new EditConflict(rejected);
            }
            throw rejected;
        }
    }

    private static boolean isDuplicateIdentity(DataIntegrityViolationException rejected)
    {
        return NestedExceptionUtils.getMostSpecificCause(rejected) instanceof SQLException cause
            && UNIQUE_VIOLATION.equals(cause.getSQLState());
    }
}
