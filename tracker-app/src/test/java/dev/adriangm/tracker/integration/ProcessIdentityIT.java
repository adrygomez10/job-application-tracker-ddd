package dev.adriangm.tracker.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.adriangm.tracker.applications.application.EditConflict;
import dev.adriangm.tracker.search.application.ManageOpportunities;
import dev.adriangm.tracker.search.domain.OpportunityDetails;
import dev.adriangm.tracker.search.domain.WorkMode;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ProcessIdentityIT extends IntegrationTest {
    @Autowired
    ManageOpportunities manage;

    @Test
    void reviewPreservesNotesAndRejectsStaleVersions()
    {
        var legacy = seed();
        UUID opportunity = UUID.randomUUID();
        manage.capture(opportunity, new OpportunityDetails("Example", "", "Java", "", WorkMode.UNSPECIFIED, null, ""));
        manage.relate(opportunity, legacy.id(), legacy.version());
        assertThat(read(legacy.id()).details()).isEqualTo(legacy.details());
        assertThat(
            jdbc.queryForObject("SELECT opportunity_id FROM job_applications WHERE id = ?", UUID.class, legacy.id())
        ).isEqualTo(opportunity);
        assertThat(
            jdbc
                .queryForObject(
                    "SELECT count(*) FROM process_opportunity_history WHERE process_id = ?",
                    Integer.class,
                    legacy.id()
                )
        ).isEqualTo(1);
        assertThatThrownBy(() -> manage.relate(opportunity, legacy.id(), legacy.version()))
            .isInstanceOf(EditConflict.class);
    }
}
