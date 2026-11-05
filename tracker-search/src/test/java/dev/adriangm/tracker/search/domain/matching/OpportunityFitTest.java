package dev.adriangm.tracker.search.domain.matching;

import static dev.adriangm.tracker.search.support.SalaryFixtures.salary;
import static org.assertj.core.api.Assertions.assertThat;

import dev.adriangm.tracker.search.domain.SalaryRange;
import dev.adriangm.tracker.search.domain.WorkMode;
import dev.adriangm.tracker.search.domain.opportunity.Opportunity;
import dev.adriangm.tracker.search.domain.opportunity.OpportunityDetails;
import dev.adriangm.tracker.search.domain.profile.AutomationSettings;
import dev.adriangm.tracker.search.domain.profile.SearchProfile;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OpportunityFitTest {
    private final SearchProfile profile = new SearchProfile(
        0,
        "Owner",
        "Java",
        "Madrid",
        WorkMode.REMOTE,
        salary("45000", null, "EUR", SalaryRange.Period.YEAR),
        ZoneId.of("Europe/Madrid"),
        false,
        LocalTime.of(22, 0),
        LocalTime.of(8, 0),
        AutomationSettings.defaults()
    );

    @Test
    void distinguishesConfirmedShortfallFromAnUnconfirmedOpenRange()
    {
        var fit = new OpportunityFit();
        assertThat(fit.assess(details(salary("30000", "40000", "EUR", SalaryRange.Period.YEAR)), profile))
            .containsExactly("Salario por debajo del objetivo");
        assertThat(fit.assess(details(salary("30000", null, "EUR", SalaryRange.Period.YEAR)), profile))
            .containsExactly("Objetivo salarial pendiente de confirmar");
        assertThat(fit.assess(details(salary("45000", null, "EUR", SalaryRange.Period.YEAR)), profile)).isEmpty();
    }

    @Test
    void missingOrIncompatibleSalaryRemainsUnknown()
    {
        assertThat(new OpportunityFit().assess(details(null), profile))
            .containsExactly("Salario sin datos comparables");
        assertThat(new OpportunityFit().assess(details(salary("50000", null, "USD", SalaryRange.Period.YEAR)), profile))
            .containsExactly("Salario sin datos comparables");
    }

    @Test
    void revisionKeepsIdentityAndProvenance()
    {
        UUID id = UUID.randomUUID();
        var original = Opportunity.capture(id, details(null), "greenhouse:example:42");
        var revised = original.revise(details(profile.targetSalary()));
        assertThat(revised.id()).isEqualTo(id);
        assertThat(revised.sourceKey()).isEqualTo(original.sourceKey());
        assertThat(original.details().salary()).isNull();
    }

    private static OpportunityDetails details(SalaryRange salary)
    {
        return new OpportunityDetails(
            "Example",
            "Agency",
            "Java Developer",
            "Madrid",
            WorkMode.REMOTE,
            salary,
            "https://example.test/job/1"
        );
    }
}
