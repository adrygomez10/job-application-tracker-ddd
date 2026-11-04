package dev.adriangm.tracker.search.adapter.web;

import dev.adriangm.tracker.search.domain.OpportunityDetails;
import dev.adriangm.tracker.search.domain.WorkMode;
import java.util.UUID;

public record OpportunityForm(
    UUID id,
    Long version,
    String employer,
    String agency,
    String role,
    String location,
    WorkMode mode,
    SalaryForm salary,
    String url
) {
    public OpportunityDetails details()
    {
        return new OpportunityDetails(employer, agency, role, location, mode, salary.toDomain(), url);
    }
}
