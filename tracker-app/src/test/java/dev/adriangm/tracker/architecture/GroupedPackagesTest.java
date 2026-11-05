package dev.adriangm.tracker.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import dev.adriangm.tracker.architecture.fixture.IllegalAggregateAccess;
import java.util.List;
import org.junit.jupiter.api.Test;

class GroupedPackagesTest {
    @Test
    void domainGroupsHaveNoCycles()
    {
        slices().matching("..applications.domain.(*)..").should().beFreeOfCycles().check(ArchitectureClasses.ALL);
        slices().matching("..search.domain.(*)..").should().beFreeOfCycles().check(ArchitectureClasses.ALL);
    }

    @Test
    void applicationGroupsHaveNoCycles()
    {
        for (String module : List.of("applications", "search", "notifications")) {
            slices()
                .matching(".." + module + ".application.(*)..")
                .should()
                .beFreeOfCycles()
                .check(ArchitectureClasses.ALL);
        }
    }

    @Test
    void adapterGroupsHaveNoCycles()
    {
        for (String module : List.of("applications", "search")) {
            for (String adapter : List.of("web", "persistence")) {
                slices()
                    .matching(".." + module + ".adapter." + adapter + ".(*)..")
                    .should()
                    .beFreeOfCycles()
                    .check(ArchitectureClasses.ALL);
            }
        }
    }

    @Test
    void onlyTheAggregateRootEditsActivities()
    {
        DomainBoundaryRules.TASK_EDITS.check(ArchitectureClasses.ALL);
        DomainBoundaryRules.INTERVIEW_EDITS.check(ArchitectureClasses.ALL);
    }

    @Test
    void onlyTheAggregateRootCreatesHistory()
    {
        DomainBoundaryRules.HISTORY_ENTRIES.check(ArchitectureClasses.ALL);
        DomainBoundaryRules.STATUS_HISTORY.check(ArchitectureClasses.ALL);
    }

    @Test
    void validationHelpersStayInsideTheirDomain()
    {
        for (String module : List.of("applications", "search")) {
            noClasses()
                .that()
                .resideOutsideOfPackage(".." + module + ".domain..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage(".." + module + ".domain.validation..")
                .check(ArchitectureClasses.ALL);
        }
    }

    @Test
    void persistenceMappingStaysInsideItsAdapter()
    {
        noClasses()
            .that()
            .resideOutsideOfPackage("..search.adapter.persistence..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..search.adapter.persistence.mapping..")
            .check(ArchitectureClasses.ALL);
    }

    @Test
    void aggregateRulesRejectPublicMethodBypasses()
    {
        var fixture = new ClassFileImporter().importClasses(IllegalAggregateAccess.class);
        assertThat(DomainBoundaryRules.TASK_EDITS.evaluate(fixture).hasViolation()).isTrue();
        assertThat(DomainBoundaryRules.INTERVIEW_EDITS.evaluate(fixture).hasViolation()).isTrue();
        assertThat(DomainBoundaryRules.HISTORY_ENTRIES.evaluate(fixture).hasViolation()).isTrue();
        assertThat(DomainBoundaryRules.STATUS_HISTORY.evaluate(fixture).hasViolation()).isTrue();
    }
}
