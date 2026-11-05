package dev.adriangm.tracker.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.Test;

class DddArchitectureTest {
    @Test
    void consumersOnlyUsePublicNotificationPreferences()
    {
        noClasses()
            .that()
            .resideOutsideOfPackages("..search..", "..configuration..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..search.domain..", "..search.application..", "..search.adapter..")
            .check(ArchitectureClasses.ALL);
    }

    @Test
    void consumersOnlyUsePublicReminderContract()
    {
        noClasses()
            .that()
            .resideOutsideOfPackages("..reminders..", "..configuration..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..reminders.application..", "..reminders.adapter..")
            .check(ArchitectureClasses.ALL);
    }

    @Test
    void consumersOnlyUsePublicNotificationContracts()
    {
        noClasses()
            .that()
            .resideOutsideOfPackages("..notifications..", "..configuration..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "..notifications.domain..",
                "..notifications.application..",
                "..notifications.adapter.."
            )
            .check(ArchitectureClasses.ALL);
    }

    @Test
    void notificationsDoNotDependOnSearch()
    {
        noClasses()
            .that()
            .resideInAPackage("..notifications..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..search..")
            .check(ArchitectureClasses.ALL);
    }

    @Test
    void onlyPersistenceCanReconstituteOpportunities()
    {
        noClasses()
            .that()
            .resideOutsideOfPackages("..search.domain..", "..search.adapter.persistence..")
            .should()
            .callMethod(
                dev.adriangm.tracker.search.domain.opportunity.Opportunity.class,
                "reconstitute",
                java.util.UUID.class,
                long.class,
                dev.adriangm.tracker.search.domain.opportunity.OpportunityDetails.class,
                String.class
            )
            .check(ArchitectureClasses.ALL);
    }
}
