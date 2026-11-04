package dev.adriangm.tracker.integration;

import static dev.adriangm.tracker.support.Fixtures.saved;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import dev.adriangm.tracker.applications.application.ApplicationStore;
import dev.adriangm.tracker.applications.application.UnitOfWork;
import dev.adriangm.tracker.applications.application.query.ApplicationQueries;
import dev.adriangm.tracker.applications.domain.JobApplication;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Shared setup for tests against the real application and PostgreSQL. Every subclass reuses the
 * same Spring context and starts from empty tables.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"tracker.reminders.interval-ms=3600000", "tracker.automation.interval-ms=3600000",
        "server.shutdown=immediate", "spring.lifecycle.timeout-per-shutdown-phase=5s"}
)
abstract class IntegrationTest {
    static final String USER = "test-owner";
    static final String PASSWORD = "test-only-password";
    static final String HASH = "{bcrypt}" + new BCryptPasswordEncoder(4).encode(PASSWORD);

    @DynamicPropertySource
    static void credentials(DynamicPropertyRegistry registry)
    {
        registry.add("tracker.username", () -> USER);
        registry.add("tracker.password-hash", () -> HASH);
    }

    @Autowired
    ApplicationStore store;
    @Autowired
    UnitOfWork work;
    @Autowired
    ApplicationQueries queries;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    WebApplicationContext context;
    @LocalServerPort
    int port;
    MockMvc mvc;

    @BeforeEach
    void reset()
    {
        jdbc.execute("TRUNCATE job_applications, imported_offers, reminders CASCADE");
        jdbc.update("UPDATE reminder_refresh SET checked_at = NULL WHERE id = 1");
        jdbc.execute("TRUNCATE notification_queue, notification_deliveries, feed_runs, opportunities CASCADE");
        jdbc.update("""
            UPDATE search_profile SET external_notifications=false, auto_import=false,
            import_interval=60, auto_notifications=false, notify_tasks=true, notify_interviews=true,
            notify_opportunities=true, quiet_from='22:00', quiet_until='08:00'
            """);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    void loginOwner(Page page)
    {
        page.navigate("http://localhost:" + port + "/login");
        page.getByLabel("Usuario").fill(USER);
        page.getByLabel("Contraseña").fill(PASSWORD);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Entrar")).click();
    }

    JobApplication seed()
    {
        return work.required(() -> store.create(saved()));
    }

    JobApplication read(UUID id)
    {
        return queries.detail(id).orElseThrow();
    }
}
