package dev.adriangm.tracker.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.adriangm.tracker.applications.api.ApplicationsApi;
import dev.adriangm.tracker.applications.application.EditConflict;
import dev.adriangm.tracker.applications.domain.Status;
import dev.adriangm.tracker.applications.domain.StatusChange;
import dev.adriangm.tracker.notifications.api.NotificationMessage;
import dev.adriangm.tracker.notifications.application.NotificationQueue;
import dev.adriangm.tracker.notifications.domain.DeliveryState;
import dev.adriangm.tracker.search.application.ManageOpportunities;
import dev.adriangm.tracker.search.application.OpportunityRepository;
import dev.adriangm.tracker.search.application.SearchConflict;
import dev.adriangm.tracker.search.domain.OpportunityDetails;
import dev.adriangm.tracker.search.domain.WorkMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DddWorkshopIT extends IntegrationTest {
    @Autowired
    OpportunityRepository search;
    @Autowired
    dev.adriangm.tracker.search.application.ProfileRepository profiles;
    @Autowired
    ManageOpportunities manage;
    @Autowired
    ApplicationsApi applications;
    @Autowired
    NotificationQueue deliveries;

    @BeforeEach
    void resetNewData()
    {
        jdbc.execute("TRUNCATE opportunities, notification_deliveries CASCADE");
        jdbc.update("UPDATE search_profile SET version=0, external_notifications=false WHERE id=1");
    }

    @Test
    void oneOpportunityCanHaveSuccessiveButNeverConcurrentActiveProcesses()
    {
        UUID opportunity = capture();
        UUID first = manage.start(opportunity, UUID.randomUUID(), "direct");
        assertThatThrownBy(() -> manage.start(opportunity, UUID.randomUUID(), "agency"))
            .hasMessageContaining("activeOpportunity");
        work.required(() -> {
            var current = store.find(first).orElseThrow();
            store
                .save(
                    current
                        .changeStatus(
                            new StatusChange(Status.REJECTED, current.appliedOn(), "", StatusChange.Kind.UPDATE),
                            Instant.now(),
                            LocalDate.now()
                        ),
                    current.version()
                );
            return null;
        });
        UUID next = manage.start(opportunity, UUID.randomUUID(), "agency");
        assertThat(next).isNotEqualTo(first);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM job_applications", Integer.class)).isEqualTo(2);
    }

    @Test
    void databaseArbitratesTwoIndependentTransactions() throws Exception
    {
        UUID opportunity = capture();
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        Callable<Boolean> contender = () -> {
            ready.countDown();
            start.await();
            try {
                manage.start(opportunity, UUID.randomUUID(), "direct");
                return true;
            } catch (EditConflict | dev.adriangm.tracker.applications.domain.RuleViolation rejected) {
                return false;
            }
        };
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(contender);
            var second = pool.submit(contender);
            ready.await();
            start.countDown();
            assertThat(java.util.List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM job_applications", Integer.class)).isEqualTo(1);
    }

    @Test
    void rejectsStaleOpportunityAndProfileWrites()
    {
        UUID id = capture();
        var first = search.find(id).orElseThrow();
        search.save(first.revise(details("Senior Java")));
        assertThatThrownBy(() -> search.save(first)).isInstanceOf(SearchConflict.class);
        var profile = profiles.load();
        profiles.save(profile);
        assertThatThrownBy(() -> profiles.save(profile)).isInstanceOf(SearchConflict.class);
    }

    @Test
    void notificationClaimSurvivesAnUncertainResultAndBlocksRepeatAttempts()
    {
        UUID activity = UUID.randomUUID();
        Instant due = Instant.parse("2026-10-24T10:00:00Z");
        var message = NotificationMessage.reminder(activity, "TASK", due, false, due);
        deliveries.enqueue(message);
        assertThat(deliveries.claim(message.key(), due)).isTrue();
        deliveries.finish(message.key(), DeliveryState.UNCERTAIN, due);
        assertThat(deliveries.claim(message.key(), due)).isFalse();
        assertThat(deliveries.history(0).getFirst().state()).isEqualTo(DeliveryState.UNCERTAIN);
    }

    @Test
    void screensAreProtectedAndFormsBindSalaryAndProfile() throws Exception
    {
        mvc.perform(get("/opportunities")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/opportunities").with(user(USER))).andExpect(status().isForbidden());
        mvc
            .perform(
                post("/opportunities")
                    .with(user(USER))
                    .with(csrf())
                    .param("id", UUID.randomUUID().toString())
                    .param("employer", "Example")
                    .param("agency", "Agency")
                    .param("role", "Java")
                    .param("location", "Madrid")
                    .param("mode", "REMOTE")
                    .param("salary.minimum", "45000")
                    .param("salary.maximum", "55000")
                    .param("salary.currency", "EUR")
                    .param("salary.period", "YEAR")
                    .param("url", "https://example.test/job")
            )
            .andExpect(status().is3xxRedirection());
        assertThat(search.recent().getFirst().details().salary().minimum()).isEqualByComparingTo("45000");
        for (String path : java.util.List.of("/opportunities", "/profile", "/notifications")) {
            mvc.perform(get(path).with(user(USER))).andExpect(status().isOk());
        }
    }

    private UUID capture()
    {
        UUID id = UUID.randomUUID();
        manage.capture(id, details("Java"));
        return id;
    }

    private static OpportunityDetails details(String role)
    {
        return new OpportunityDetails(
            "Example",
            "Agency",
            role,
            "Madrid",
            WorkMode.REMOTE,
            null,
            "https://example.test/job"
        );
    }
}
