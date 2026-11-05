package dev.adriangm.tracker.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import dev.adriangm.tracker.applications.application.port.ApplicationStore;
import dev.adriangm.tracker.applications.application.port.UnitOfWork;
import dev.adriangm.tracker.applications.application.support.VersionedUpdate;
import dev.adriangm.tracker.applications.domain.activity.FollowUpTask;
import dev.adriangm.tracker.applications.domain.activity.Interview;
import dev.adriangm.tracker.applications.domain.candidacy.JobApplication;
import dev.adriangm.tracker.applications.domain.history.TimelineEntry;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ArchitectureTest {
    private static final com.tngtech.archunit.core.domain.JavaClasses CLASSES = ArchitectureClasses.ALL;

    @Test
    void domainOnlyDependsOnJavaAndItself()
    {
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideOutsideOfPackages("java..", "..domain..")
            .check(CLASSES);
    }

    @Test
    void applicationDoesNotKnowFrameworksOrAdapters()
    {
        noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideOutsideOfPackages("java..", "..domain..", "..application..", "..api..")
            .check(CLASSES);
    }

    @Test
    void webCannotImportJpa()
    {
        noClasses()
            .that()
            .resideInAPackage("..adapter.web..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..adapter.persistence..", "jakarta.persistence..", "org.hibernate..")
            .check(CLASSES);
    }

    @Test
    void persistenceCannotImportWeb()
    {
        noClasses()
            .that()
            .resideInAPackage("..adapter.persistence..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..adapter.web..")
            .check(CLASSES);
    }

    @Test
    void featureSlicesDoNotDependOnEachOther()
    {
        var features = List.of("activities", "details", "status", "query");
        for (String feature : features) {
            String[] others = features
                .stream()
                .filter(other -> !other.equals(feature))
                .map(other -> "..applications.application." + other + "..")
                .toArray(String[]::new);
            noClasses()
                .that()
                .resideInAPackage("..applications.application." + feature + "..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(others)
                .check(CLASSES);
        }
    }

    @Test
    void webOnlyTalksToUseCasesAndQueries()
    {
        noClasses()
            .that()
            .resideInAPackage("..adapter.web..")
            .should()
            .dependOnClassesThat()
            .belongToAnyOf(ApplicationStore.class, UnitOfWork.class, VersionedUpdate.class)
            .check(CLASSES);
    }

    @Test
    void onlyDomainAndPersistenceRebuildSnapshots()
    {
        var snapshots = Set
            .of(JobApplication.class, Interview.class, FollowUpTask.class, TimelineEntry.class)
            .stream()
            .map(Class::getName)
            .toList();
        noClasses()
            .that()
            .resideOutsideOfPackages("..domain..", "..adapter.persistence..")
            .should()
            .callConstructorWhere(
                DescribedPredicate
                    .describe(
                        "a snapshot constructor",
                        (JavaConstructorCall call) -> snapshots.contains(call.getTargetOwner().getName())
                    )
            )
            .check(CLASSES);
    }

    @Test
    void configurationIsOnlyAnAssemblyRoot()
    {
        noClasses()
            .that()
            .resideOutsideOfPackage("..configuration..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..configuration..")
            .check(CLASSES);
    }

    @Test
    void productionUsesConstructorInjection()
    {
        noFields().should().beAnnotatedWith(Autowired.class).check(CLASSES);
    }
}
